package io.github.tatooinoyo.star.badge.service

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.MainActivity
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.service.component.DrawerMenu
import io.github.tatooinoyo.star.badge.service.component.DrawerMenuItem
import io.github.tatooinoyo.star.badge.service.component.FloatingWidget
import io.github.tatooinoyo.star.badge.service.manager.FloatingWindowManager
import io.github.tatooinoyo.star.badge.service.utils.ServiceLifecycleOwner
import kotlinx.coroutines.launch

class FloatingButtonService : Service() {

    // 1. 使用封装好的 LifecycleOwner
    private val lifecycleOwner = ServiceLifecycleOwner()

    // 2. 使用封装好的 WindowManager
    private lateinit var windowManagerHelper: FloatingWindowManager

    // UI 变量
    private lateinit var floatingButtonView: View
    private var menuView: View? = null
    private var isMenuOpen = false

    private var selectedTag by mutableStateOf<String?>(null)
    private var menuLazyListState: LazyListState? = null
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
                // 1. 获取当前配置
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                // 2. 根据横竖屏状态控制显示隐藏
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.LaunchedEffect(isLandscape) {
                    // 只在横屏时显示
                    view.visibility = if (isLandscape) View.VISIBLE else View.GONE
                    // 竖屏时关闭菜单（如果打开的话）
                    if (!isLandscape && isMenuOpen) {
                        closeMenu()
                    }
                }

                // 只在横屏时渲染内容
                if (isLandscape) {
                    FloatingWidget(
                        onClick = { toggleMenu() },
                    )
                }
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

                // 使用 remember，但初始化逻辑指向 Service 成员变量
                val scrollState = remember {
                    menuLazyListState ?: LazyListState().also {
                        menuLazyListState = it
                    }
                }
                val scope = rememberCoroutineScope()

                val allTags = remember(badgeList) {
                    badgeList.flatMap { it.tags }.distinct().sorted()
                }
                val displayBadges = remember(badgeList, selectedTag) {
                    if (selectedTag == null) {
                        badgeList
                    } else {
                        badgeList.filter { it.tags.contains(selectedTag) }
                    }
                }
                val dynamicMenuItems = displayBadges.map { badge ->
                    DrawerMenuItem(
                        id = badge.id,
                        title = badge.title,
                        icon = Icons.Default.Star,
                        channel = badge.channel,
                        remark = badge.remark
                    ) {
                        Toast.makeText(context, "选中: ${badge.title}", Toast.LENGTH_SHORT).show()
                        badge.usage(context)
                    }
                }

                Surface(
                    modifier = Modifier.width(200.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    DrawerMenu(
                        items = dynamicMenuItems,
                        lazyListState = scrollState,
                        allTags = allTags,
                        selectedTag = selectedTag,
                        onTagSelected = { tag ->
                            selectedTag = tag
                            // 筛选时回到顶部
                            scope.launch { scrollState.scrollToItem(0) }
                        },
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