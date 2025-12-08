package io.tatooinoyo.star.badge.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import io.tatooinoyo.star.badge.MainActivity
import io.tatooinoyo.star.badge.data.BadgeRepository
import io.tatooinoyo.star.badge.service.manager.FloatingWindowManager
import io.tatooinoyo.star.badge.service.utils.ServiceLifecycleOwner
import io.tatooinoyo.star.badge.ui.component.DrawerMenu
import io.tatooinoyo.star.badge.ui.component.DrawerMenuItem
import io.tatooinoyo.star.badge.ui.component.FloatingWidget

class FloatingButtonService : Service() {

    // 1. 使用封装好的 LifecycleOwner
    private val lifecycleOwner = ServiceLifecycleOwner()
    // 2. 使用封装好的 WindowManager
    private lateinit var windowManagerHelper: FloatingWindowManager
    
    // UI 变量
    private lateinit var floatingButtonView: View
    private var menuView: View? = null
    private var isMenuOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        windowManagerHelper = FloatingWindowManager(this)
        
        setupFloatingButton()
    }

    private fun setupFloatingButton() {
        floatingButtonView = ComposeView(this).apply {
            // 一行代码绑定生命周期
            lifecycleOwner.attachToView(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { 
                FloatingWidget(onClick = { toggleMenu() }) 
            }
        }
        // 委托给 Helper 添加 View
        windowManagerHelper.addFloatingButton(floatingButtonView)
    }

    private fun toggleMenu() {
        if (isMenuOpen) closeMenu() else openMenu()
    }

    private fun openMenu() {
        if (isMenuOpen) return

        if (menuView == null) {
            createMenuView()
        }

        try {
            // 使用 Helper 添加菜单 View
            windowManagerHelper.addMenu(menuView!!)
            isMenuOpen = true

            menuView?.let { view ->
                // 动画逻辑保持不变
                view.post {
                    view.translationX = view.width.toFloat()
                    view.visibility = View.VISIBLE
                    view.animate()
                        .translationX(0f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
            floatingButtonView.visibility = View.INVISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeMenu() {
        if (!isMenuOpen || menuView == null) return

        menuView?.animate()
            ?.translationX(menuView!!.width.toFloat())
            ?.setDuration(300)
            ?.withEndAction {
                try {
                    menuView?.visibility = View.GONE
                    windowManagerHelper.removeView(menuView)
                    menuView = null

                    isMenuOpen = false
                    floatingButtonView.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ?.start()
    }

    private fun createMenuView() {
        menuView = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            visibility = View.INVISIBLE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                val badgeList by BadgeRepository.badges.collectAsState()
                val dynamicMenuItems = badgeList.map { badge ->
                    DrawerMenuItem(
                        id = badge.id,
                        title = badge.title,
                        icon = Icons.Default.Star,
                        remark = badge.remark
                    ) {
                        Toast.makeText(context, "选中: ${badge.title}", Toast.LENGTH_SHORT).show()
                        badge.usage(context)
                    }
                }

                Surface(
                    modifier = Modifier.width(160.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    DrawerMenu(
                        items = dynamicMenuItems,
                        onGoHomeClick = ::navigateToHome,
                        onCloseClick = ::closeMenu
                    )
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        closeMenu()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onDestroy()
        if (::floatingButtonView.isInitialized) {
            windowManagerHelper.removeView(floatingButtonView)
        }
        if (menuView != null) {
            windowManagerHelper.removeView(menuView)
        }
    }
}