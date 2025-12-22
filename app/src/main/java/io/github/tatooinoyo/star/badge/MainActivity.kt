package io.github.tatooinoyo.star.badge

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.service.FloatingButtonService
import io.github.tatooinoyo.star.badge.ui.home.BadgeManagerScreen
import io.github.tatooinoyo.star.badge.ui.home.BadgeManagerViewModel
import io.github.tatooinoyo.star.badge.utils.LanguageManager
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    // 创建一个全局状态来持有读取到的 NFC 数据
    // 使用 mutableStateOf 让 Compose 可以感知变化
    private var scannedNfcData by mutableStateOf<String?>(null)
    private var nfcAdapter: NfcAdapter? = null

    // 全局 ViewModel，方便 Activity 直接调用写入逻辑
    // 注意：在正式架构中可能需要更好的方式（例如依赖注入），这里为了简单直接在 Compose 内部和 Activity 之间共享
    // 但为了确保 BadgeManagerScreen 能获取到同一个 ViewModel 实例，最简单的是让 Compose 自己创建，
    // 或者我们在这里创建传进去。
    // 为了支持 "Activity 接收到 Tag -> ViewModel.writeNfcTag"，我们需要能访问到 ViewModel。
    // 这里我们依然让 Compose 拥有 ViewModel，但我们可以通过回调或者共享对象来传递 Tag。

    // 实际上，ViewModel 是依附于 Activity 的 ViewModelStore 的。
    // 我们可以在 onNewIntent 中获取 ViewModel 实例（如果在 Activity 中获取）。
    // 或者更简单的，我们定义一个全局的回调，当 Tag 被发现时，如果正处于写入模式，则调用。

    // 增加一个辅助属性，方便判断是否支持 NFC
    private val isNfcSupported: Boolean
        get() = nfcAdapter != null

    // 临时的 Tag 变量
    private var currentTag: Tag? = null

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
        // 应用语言设置
        LanguageManager.getInstance(this).applyLanguage(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 初始化数据库
        BadgeRepository.initialize(applicationContext)
        // 初始化 NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 获取 ViewModel 实例，它会自动从 Activity 的 ViewModelStore 中获取
                    val viewModel: BadgeManagerViewModel = viewModel()

                    // 当发现新的 Tag 时，尝试写入
                    // 注意：这里是一个副作用，当 currentTag 更新时触发
                    // 但更合理的做法是让 ViewModel 处理业务，Activity 只负责传递事件

                    BadgeManagerScreen(
                        nfcPayload = scannedNfcData,
                        onNfcDataConsumed = { scannedNfcData = null },
                        viewModel = viewModel
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
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // 添加 TAG_DISCOVERED 和 TECH_DISCOVERED 作为兜底，确保能捕获所有类型的 NFC 标签
        val nfcFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try {
                    addDataType("*/*") // 监听所有类型的 NDEF 数据
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                    throw RuntimeException("fail", e)
                }
            },
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
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
        // 扩展判断条件，处理 TAG_DISCOVERED 和 TECH_DISCOVERED
        if (isNfcSupported && (
                    NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
                            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
                            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                    )
        ) {
            handleNfcIntent(intent)
        }
    }

    // 解析 NFC 数据的方法
    private fun handleNfcIntent(intent: Intent) {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        // 尝试获取 Activity 作用域的 ViewModel
        // 注意：这需要在 UI 线程中执行
        if (tag != null) {
            // 简单的办法：通过 ViewModelProvider 获取 Activity 的 ViewModel 实例
            // 但在 Activity 中直接这样写比较 hacky，通常是在 Fragment 或 Compose 中获取。
            // 这里我们使用一个简单的技巧：我们假设 MainActivity 是 SingleTop 的，
            // 且 Compose 已经初始化。

            // 为了将 Tag 传递给 ViewModel，我们可以使用一个更稳健的方法。
            // 由于 Compose 树中的 viewModel() 也是获取 Activity 范围的（默认情况），
            // 我们可以直接再次获取它。
            try {
                val viewModel =
                    androidx.lifecycle.ViewModelProvider(this)[BadgeManagerViewModel::class.java]
                if (viewModel.uiState.value.isWritingNfc) {
                    val success = viewModel.writeNfcTag(tag, this)
                    if (success) {
                        // 写入成功，不需要继续解析读取了
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
                        Toast.makeText(this, R.string.nfc_read_success, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // 如果不是 NDEF 数据（例如未格式化的卡片），依然提示用户
            // 这里可以扩展去读取卡片 ID: intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
            val id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
            val idHex = id?.joinToString("") { "%02x".format(it) } ?: "Unknown"
            scannedNfcData = "Card ID: $idHex (Raw Tag)"
            Toast.makeText(this, getString(R.string.nfc_card_detected, idHex), Toast.LENGTH_SHORT)
                .show()
        }
    }

    // 简单的 Payload 解析器 (支持 Text 和 URI)
    private fun parsePayload(record: NdefRecord): String {
        return try {
            val payload = record.payload

            // 检查是否是 URI 记录 (TNF_WELL_KNOWN + RTD_URI)
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && java.util.Arrays.equals(
                    record.type,
                    NdefRecord.RTD_URI
                )
            ) {
                // URI 记录的第一个字节是前缀代码
                val prefixCode = payload[0].toInt()
                val prefix = when (prefixCode) {
                    0x00 -> ""
                    0x01 -> "http://www."
                    0x02 -> "https://www."
                    0x03 -> "http://"
                    0x04 -> "https://"
                    0x05 -> "tel:"
                    0x06 -> "mailto:"
                    0x07 -> "ftp://anonymous:anonymous@"
                    0x08 -> "ftp://ftp."
                    0x09 -> "ftps://"
                    0x0A -> "sftp://"
                    0x0B -> "smb://"
                    0x0C -> "nfs://"
                    0x0D -> "ftp://"
                    0x0E -> "dav://"
                    0x0F -> "news:"
                    0x10 -> "telnet://"
                    0x11 -> "imap:"
                    0x12 -> "rtsp://"
                    0x13 -> "urn:"
                    0x14 -> "pop:"
                    0x15 -> "sip:"
                    0x16 -> "sips:"
                    0x17 -> "tftp:"
                    0x18 -> "btspp://"
                    0x19 -> "btl2cap://"
                    0x1A -> "btgoep://"
                    0x1B -> "tcpobex://"
                    0x1C -> "irdaobex://"
                    0x1D -> "file://"
                    0x1E -> "urn:epc:id:"
                    0x1F -> "urn:epc:tag:"
                    0x20 -> "urn:epc:pat:"
                    0x21 -> "urn:epc:raw:"
                    0x22 -> "urn:epc:"
                    0x23 -> "urn:nfc:"
                    else -> ""
                }
                // 剩余部分是实际的 URI 内容
                val uriContent = String(payload, 1, payload.size - 1, StandardCharsets.UTF_8)
                return prefix + uriContent
            }
            // 检查是否是 Text 记录 (TNF_WELL_KNOWN + RTD_TEXT)
            else if (record.tnf == NdefRecord.TNF_WELL_KNOWN && java.util.Arrays.equals(
                    record.type,
                    NdefRecord.RTD_TEXT
                )
            ) {
                // Text 记录解析
                val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
                val languageCodeLength = payload[0].toInt() and 63
                return String(
                    payload,
                    languageCodeLength + 1,
                    payload.size - languageCodeLength - 1,
                    java.nio.charset.Charset.forName(textEncoding)
                )
            } else {
                // 其他类型尝试直接转 String
                String(record.payload)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果解析失败，尝试直接转 String
            String(record.payload)
        }
    }

    // 检查是否有悬浮窗权限
    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    // 请求权限
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
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
