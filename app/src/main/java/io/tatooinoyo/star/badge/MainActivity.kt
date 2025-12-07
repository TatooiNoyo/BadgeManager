package io.tatooinoyo.star.badge

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.Modifier
import io.tatooinoyo.star.badge.service.FloatingButtonService
import io.tatooinoyo.star.badge.ui.screen.BadgeManagerScreen

class MainActivity : ComponentActivity() {

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
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BadgeManagerScreen()
                }
            }
        }

        // ==== 新增逻辑：检查并请求权限，然后开启悬浮窗 ====
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
        } else {
            startFloatingService()
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
