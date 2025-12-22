package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SecureChannel(
    private val socket: Socket,
    private val sessionKey: ByteArray,
) {
    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()
    private val recvExecutor = Executors.newSingleThreadExecutor()
    private val sendExecutor = Executors.newSingleThreadExecutor()

    private var running = true

    fun startReceiving(
        onMessage: (String) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        recvExecutor.submit {
            try {
                val headerBuffer = ByteArray(4)
                while (running) {
                    // 读 4 字节长度
                    readFully(input, headerBuffer, 4)
                    val length = ByteBuffer.wrap(headerBuffer).int

                    // 读 payload & 解密
                    val payload = ByteArray(length)
                    readFully(input, payload, length)
                    val plaintext = CryptoUtil.aesGcmDecrypt(sessionKey, payload)
                    val msg = String(plaintext, Charsets.UTF_8)

                    val obj = JSONObject(msg)
                    when (obj.getString("type")) {
                        "data" -> onMessage(obj.optString("data"))
                        "disconnect" -> {
                            Log.d("startReceiving", "对端主动断开: ${obj.optString("reason")}")
                            disconnect(notifyPeer = false)
                        }
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    Log.w("startReceiving", e.stackTraceToString())
                    onError(e)
                }
                disconnect(notifyPeer = false)
            }
        }
    }

    data class BaseResult(val type: String, val data: String?, val reason: String?)

    fun sendData(result: BaseResult, onSendSuccess: () -> Unit = {}) {
        // 如果已经不是 running 状态，且发的不是 disconnect 消息，则拒绝发送
        if (!running && result.type != "disconnect") return

        sendExecutor.submit {
            try {
                val gson = Gson()
                val resultJson = gson.toJson(result)
                val plaintext = resultJson.toByteArray(Charsets.UTF_8)
                val payload = CryptoUtil.aesGcmEncrypt(sessionKey, plaintext)
                Log.d("SecureChannel::sendData", "payload size: ${payload.size}")
                val header = ByteBuffer.allocate(4).putInt(payload.size).array()

                synchronized(output) {
                    if (!socket.isClosed) {

                        output.write(header)
                        output.write(payload)
                        output.flush()
                        Log.d("SecureChannel::sendData", "发送成功")
                        onSendSuccess()
                    } else {
                        if (result.type == "disconnect") {
                            Log.w("SecureChannel::sendData", "socket 已关闭，无法发送数据")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果发送断开消息失败了，也应该强制关闭资源，避免挂死
                if (result.type == "disconnect") {
                    try {
                        socket.close()
                    } catch (e: Exception) {
                        Log.w("SecureChannel::sendData", e.stackTraceToString())
                    }
                    // 确保资源被回收
                    recvExecutor.shutdownNow()
                    sendExecutor.shutdownNow()
                }
            }
        }
    }

    fun disconnect(reason: String = "user_exit", notifyPeer: Boolean = true) {
        // 1. 原子性检查，防止多次调用
        synchronized(this) {
            if (!running) return
            running = false
        }        // 定义清理资源的函数
        fun closeResources() {
            Log.d("SecureChannel::disconnect#closeResources", "关闭资源")
            try {
                // 1. 先停接收，防止干扰
                recvExecutor.shutdownNow()

                // 2. 停止接收新发送任务
                sendExecutor.shutdown()

                // 3. 稍微等待一下发送队列清空（可选，但更稳健）
                 try { sendExecutor.awaitTermination(200, TimeUnit.MILLISECONDS) } catch (e: Exception) {}

                if (!socket.isClosed) {
                    socket.shutdownOutput()
                    socket.close()
                }
            } catch (e: Exception) {
                Log.w("SecureChannel::disconnect#closeResources", e.stackTraceToString())
            }
        }

        try {
            if (notifyPeer && !socket.isClosed) {
                val result = BaseResult(type = "disconnect", data = null, reason = reason)
                Log.d("SecureChannel::disconnect", "发送断开消息")
                // 2. 利用 sendData 的回调机制来关闭资源
                sendData(result, onSendSuccess = {
                    closeResources()
                })
                // 注意：这里我们依赖 sendData 内部的 try-catch。
                // 如果 sendData 因为 socket 异常没跑回调，我们需要一个保底机制。
            } else {
                // 不需要通知，直接关闭
                closeResources()
            }
        } catch (e: Exception) {
            Log.w("SecureChannel::disconnect", e.stackTraceToString())
            closeResources()
        }
    }


    private fun readFully(input: InputStream, buffer: ByteArray, length: Int) {
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read == -1) throw IOException("Stream closed")
            offset += read
        }
    }
}
