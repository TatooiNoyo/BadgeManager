package io.tatooinoyo.star.badge.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.tatooinoyo.star.badge.MainActivity
import io.tatooinoyo.star.badge.data.BadgeRepository
import io.tatooinoyo.star.badge.ui.component.DrawerMenu
import io.tatooinoyo.star.badge.ui.component.DrawerMenuItem
import io.tatooinoyo.star.badge.ui.component.FloatingWidget

class FloatingButtonService : Service(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager

    // 为了支持 ComposeView 在 Service 中运行，我们需要手动管理生命周期
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.Companion.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    //在类成员变量里加一个空的 ViewModelStore
    private val viewModelStore = ViewModelStore()

    // 1. 悬浮球相关
    private lateinit var floatingButtonView: View
    private lateinit var floatingButtonParams: WindowManager.LayoutParams

    // 2. 全局菜单相关
    private var menuView: View? = null // 菜单 View (懒加载)
    private var isMenuOpen = false
    private lateinit var menuParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 初始化悬浮球
        setupFloatingButton()
    }

    // ================== 1. 悬浮球逻辑 ==================
    private fun setupFloatingButton() {
        // 【关键修改】使用 ComposeView 替代 XML Inflater
        floatingButtonView = ComposeView(this).apply {
            // 必须为 ComposeView 设置生命周期所有者，否则会 crash
            setViewTreeLifecycleOwner(this@FloatingButtonService)
            setViewTreeSavedStateRegistryOwner(this@FloatingButtonService)

            // 【新增必须】设置 ViewModel 存储，否则 Compose 可能会静默失败
            val viewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = this@FloatingButtonService.viewModelStore
            }
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)

            // 【建议新增】设置组合策略，确保 View 销毁时释放资源
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                // 调用我们刚才写的 Compose 组件
                FloatingWidget(
                    onClick = {
                        if (isMenuOpen) closeMenu() else openMenu()
                    }
                )
            }
        }

        floatingButtonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getLayoutType(), // 适配 Android 版本
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // 允许点击后面的内容
            PixelFormat.TRANSLUCENT
        )

        floatingButtonParams.gravity = Gravity.TOP or Gravity.END
        floatingButtonParams.y = 200 // 初始位置
        floatingButtonParams.x = 0

        // 添加悬浮球到屏幕
        windowManager.addView(floatingButtonView, floatingButtonParams)
    }

    // ================== 2. 全局菜单逻辑 ==================
    private fun openMenu() {
        if (isMenuOpen) return

        if (menuView == null) {
            createMenuView()
        }

        try {
            windowManager.addView(menuView, menuParams)
            isMenuOpen = true

            // 【修改点】直接对 menuView 进行动画，不再查找 ID
            menuView?.let { view ->
                // 先把 View 移到屏幕右侧外面
                view.post {
                    view.translationX = view.width.toFloat()

                    // 移位完成后， 设为可见
                    view.visibility = View.VISIBLE
                    // 动画移回来
                    view.animate()
                        .translationX(0f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
            // 移动回来后将悬浮球设置为不可见
            floatingButtonView.visibility = View.INVISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeMenu() {
        if (!isMenuOpen || menuView == null) return

        // 【修改点】直接对 menuView 进行动画
        menuView?.animate()
            ?.translationX(menuView!!.width.toFloat()) // 移出屏幕
            ?.setDuration(300)
            ?.withEndAction {
                try {
                    // 告知渲染引擎停止渲染这个View
                    menuView?.visibility = View.GONE
                    // 动画结束后移除 Window
                    windowManager.removeView(menuView)
                    // 强制下次openMenu 重新创建新的 View
                    menuView = null

                    isMenuOpen = false
                    // 恢复悬浮球
                    floatingButtonView.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ?.start()
    }

    private fun navigateToHome() {
        // Service 本身就是 Context，直接用 this
        val intent = Intent(this, MainActivity::class.java)
        // 从 Service 启动 Activity 必须加 NEW_TASK flag
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        // 跳转后关闭菜单
        closeMenu()
    }


    private fun createMenuView() {
        // 1. 使用 ComposeView 替代 XML Inflater
        menuView = ComposeView(this).apply {
            // 设置生命周期和状态保存 (和悬浮球一样，这是必须的)
            setViewTreeLifecycleOwner(this@FloatingButtonService)
            setViewTreeSavedStateRegistryOwner(this@FloatingButtonService)

            // 设置 ViewModelStoreOwner
            val viewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = this@FloatingButtonService.viewModelStore
            }
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)

            visibility = View.INVISIBLE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                // 【关键修改 1】监听仓库数据变化
                // 当 BadgeRepository.badges 发生变化时，这里会自动重组 (Recompose)
                val badgeList by BadgeRepository.badges.collectAsState()
                // 【关键修改 2】将数据模型 (Badge) 转换为 菜单模型 (DrawerMenuItem)
                val dynamicMenuItems = badgeList.map { badge ->
                    DrawerMenuItem(
                        id = badge.id,
                        title = badge.title,
                        icon = Icons.Default.Star, // 这里可以给 Badge 加字段来区分图标，暂时统一用 Star
                        remark = badge.remark
                    ) {
                        // 点击徽章的事件：比如弹出提示，或者复制内容
                        Toast.makeText(context, "选中: ${badge.title}", Toast.LENGTH_SHORT).show()
                        badge.usage(context)
                    }
                }

                // 定义菜单项数据和点击事件
                val menuItems = dynamicMenuItems

                // 渲染 UI
                // 使用 Surface 提供背景色和阴影，并固定宽度以适配 WindowManager 的 WRAP_CONTENT
                Surface(
                    modifier = Modifier.width(160.dp), // 在这里控制菜单宽度
                    // 【修改点】使用 copy(alpha = ...) 设置透明度。0.5f 表示 50% 不透明度
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    // 添加圆角，半透明效果下圆角看起来更自然
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    DrawerMenu(
                        items = menuItems,
                        onGoHomeClick = ::navigateToHome,
                        onCloseClick = ::closeMenu
                    )
                }
            }
        }

        // 2. WindowManager 参数配置
        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // 宽度：由 Compose 的 Modifier.width 决定
            WindowManager.LayoutParams.MATCH_PARENT, // 高度：全屏
            getLayoutType(),
            // Flags: 允许穿透点击 (FLAG_NOT_TOUCH_MODAL)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // 重心靠右
        menuParams.gravity = Gravity.END or Gravity.TOP
    }


    // ================== 工具方法 ==================
    private fun getLayoutType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 停止服务，这会触发 onDestroy 并移除悬浮窗
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        // ... 移除 View 的逻辑 ...
        if (isMenuOpen && menuView != null) {
            windowManager.removeView(menuView)
        }
        if (::floatingButtonView.isInitialized) {
            windowManager.removeView(floatingButtonView)
        }
    }
}