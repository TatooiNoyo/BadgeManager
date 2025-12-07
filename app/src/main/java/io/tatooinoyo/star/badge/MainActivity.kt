package io.tatooinoyo.star.badge

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.tatooinoyo.star.badge.data.BadgeRepository
import io.tatooinoyo.star.badge.service.FloatingButtonService
import io.tatooinoyo.star.badge.ui.screen.BadgeManagerScreen

class MainActivity : ComponentActivity() {
    // 创建一个全局状态来持有读取到的 NFC 数据
    // 使用 mutableStateOf 让 Compose 可以感知变化
    private var scannedNfcData by mutableStateOf<String?>(null)
    private var nfcAdapter: NfcAdapter? = null

    // 增加一个辅助属性，方便判断是否支持 NFC
    private val isNfcSupported: Boolean
        get() = nfcAdapter != null

    // 注册权限请求回调
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (checkOverlayPermission()) {
            startFloatingService()
        } else {
            Toast.makeText(this, "需要悬浮窗权限才能显示悬浮球", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化数据库
        BadgeRepository.initialize(applicationContext)
        // 初始化 NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BadgeManagerScreen(
                        nfcPayload = scannedNfcData,
                        onNfcDataConsumed = { scannedNfcData = null }
                    )
                }
            }
        }

        // ==== 新增逻辑：检查并请求权限，然后开启悬浮窗 ====
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
        } else {
            startFloatingService()
        }

        // 处理如果是通过 NFC 标签启动 App 的情况
        if (isNfcSupported && NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            handleNfcIntent(intent)
        }
    }


    override fun onResume() {
        super.onResume()
        if (!isNfcSupported) return
        // 启用前台调度系统 (Foreground Dispatch)
        // 这样当 App 在前台时，NFC 意图会优先发给这个 Activity，而不是弹出系统选择框
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or 0 // Android 12+ 需要 FLAG_MUTABLE
        )

        val nfcFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try {
                    addDataType("*/*") // 监听所有类型的 NDEF 数据
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                    throw RuntimeException("fail", e)
                }
            }
        )

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, nfcFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // 处理新的 Intent (当在 Activity 处于前台时触碰 NFC)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNfcSupported && NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            handleNfcIntent(intent)
        }
    }

    // 解析 NFC 数据的方法
    private fun handleNfcIntent(intent: Intent) {
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMsgs != null) {
            val msgs = arrayOfNulls<NdefMessage>(rawMsgs.size)
            for (i in rawMsgs.indices) {
                msgs[i] = rawMsgs[i] as NdefMessage
            }
            // 简单起见，只读取第一条记录
            if (msgs.isNotEmpty()) {
                val record = msgs[0]?.records?.get(0)
                record?.let {
                    // 解析文本或 URI 内容
                    val payload = parsePayload(it)
                    if (payload.isNotEmpty()) {
                        scannedNfcData = payload
                        Toast.makeText(this, "NFC读取成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 简单的 Payload 解析器 (支持 Text 和 URI)
    private fun parsePayload(record: NdefRecord): String {
        try {
            val payload = record.payload

            // 简单判断是否是 URI 记录 (0x00 = 没有任何前缀, 0x01 = http://www., etc.)
            // 这里为了简化，直接按照字符串读取，实际生产环境建议使用专门的 NDEF 解析库
            // 或者处理 TextRecord 的编码位

            // 尝试直接转字符串，去除可能存在的语言编码前缀 (通常 Text Record 前几个字节是编码信息)
            val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
            val languageCodeLength = payload[0].toInt() and 63

            return String(
                payload,
                languageCodeLength + 1,
                payload.size - languageCodeLength - 1,
                java.nio.charset.Charset.forName(textEncoding)
            )
        } catch (e: Exception) {
            // 如果解析失败，尝试直接转 String
            return String(record.payload)
        }
    }

    // 检查是否有悬浮窗权限
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    // 请求权限
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    // 启动 Service
    private fun startFloatingService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
    }

    // 可选：Activity 销毁时是否要关闭悬浮窗？
    // 如果要在最小化后显示，就不要在这里 stopService
    override fun onDestroy() {
        super.onDestroy()
        // stopService(Intent(this, FloatingButtonService::class.java))
    }
}
