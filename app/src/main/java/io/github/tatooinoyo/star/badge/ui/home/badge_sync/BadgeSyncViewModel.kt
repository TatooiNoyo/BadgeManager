package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BadgeSyncViewModel(

) : ViewModel() {
    // ===局域网同步状态 ===
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private var udpListener: UdpListener? = null
    private var udpBroadcaster: UdpBroadcaster? = null
    private var tcpClient: TcpClient? = null
    private var tcpServer: TcpServer? = null
    private var tcpChannel: SecureChannel? = null


    // === 局域网同步 ===

    // 生成 6 位随机分享码
    private fun generateShareCode(): String {
        return (100000..999999).random().toString()
    }

    // === 发送端逻辑 (安全版) ===
    fun startSenderMode() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 生成 6 位分享码
            val code = generateShareCode()
            try {
                _syncState.value = SyncState.Sender.Ready(code)
                // 2. 准备数据
                val badgeJson = exportBadgesToJson()

                // 3. 开启UDP监听
                udpListener = UdpListener(onDeviceFound = { ip, targetPort ->
                    _syncState.value = SyncState.Sender.Handshaking(code, ip)
                    Log.d("startSenderMode", "onDeviceFound: $ip:$targetPort")


                    val tcpClient = TcpClient(ip, targetPort = targetPort, psk6 = code)
                    this@BadgeSyncViewModel.tcpClient = tcpClient

                    CoroutineScope(Dispatchers.IO).launch {
                        val channel = tcpClient.connectAndGetChannel()
                        if (channel != null) {
                            this@BadgeSyncViewModel.tcpChannel = channel

                            val payload = SecureChannel.BaseResult("data", badgeJson, null)
                            channel.sendData(payload) {
                                _syncState.value = SyncState.Sender.Success(code)
                            }
                        } else {
                            _syncState.value = SyncState.Sender.Error(code, "连接失败")
                            stopSenderConn()
                        }
                    }
                })
                udpListener?.start()


            } catch (e: Exception) {
                _syncState.value = SyncState.Sender.Error(code, "发送出错: ${e.message}")
                Log.w("startSenderMode", "发送出错: ${e.message}")
                stopSenderConn()
            }
        }
    }

    fun stopSenderConn() {
        udpListener?.stop()
        udpListener = null

        tcpChannel?.disconnect("主动关闭")
        tcpChannel = null

        tcpClient = null
    }

    fun stopSenderMode() {
        stopSenderConn()
        _syncState.value = SyncState.Idle
    }

    // === 接收端逻辑 (安全版) ===
    fun startReceiverMode(inputCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            udpBroadcaster = UdpBroadcaster()
            tcpServer = TcpServer(inputCode)
            _syncState.value = SyncState.Receiver.Searching
            tcpServer?.start({
                // 只有在tcp server开始监听后, 才能获取绑定的端口号
                udpBroadcaster?.start(it.toString())
            }, { channel ->
                this@BadgeSyncViewModel.tcpChannel = channel

                udpBroadcaster?.stop()
                channel.startReceiving({ badgeJsonStr ->
                    viewModelScope.launch(Dispatchers.IO) {
                        importBadgesFromJson(badgeJsonStr) { success ->
                            if (success) {
                                _syncState.value = SyncState.Receiver.Success
                            } else {
                                _syncState.value = SyncState.Receiver.Error("导入失败")
                            }
                        }
                    }
                }, { err ->
                    _syncState.value = SyncState.Receiver.Error(err.stackTraceToString())
                })
            }, { errMsg ->
                _syncState.value = SyncState.Receiver.Error(errMsg)
            })

        }
    }

    fun stopReceiverMode() {
        stopReceiverConn()
        _syncState.value = SyncState.Idle
    }

    fun stopReceiverConn() {
        udpBroadcaster?.stop()
        udpBroadcaster = null

        tcpChannel?.disconnect("主动关闭")
        tcpChannel = null

        tcpServer?.stop()
        tcpServer = null
    }


    private suspend fun exportBadgesToJson(): String {
        val badges = BadgeRepository.getAllBadgesSnapshot()
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(badges)
    }

    /**
     * 从 JSON 字符串还原数据 (供局域网同步使用)
     */
    private suspend fun importBadgesFromJson(jsonStr: String, onResult: (Boolean) -> Unit) {
        try {
            if (jsonStr.isBlank()) {
                onResult(false)
                return
            }

            val gson = Gson()
            val type = object : TypeToken<List<Badge>>() {}.type
            val badges: List<Badge> = gson.fromJson(jsonStr, type)

            if (badges.isNotEmpty()) {
                BadgeRepository.restoreBadges(badges)
                onResult(true)
            } else {
                onResult(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }

}