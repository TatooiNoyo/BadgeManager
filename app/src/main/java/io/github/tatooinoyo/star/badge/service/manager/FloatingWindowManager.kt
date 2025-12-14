package io.github.tatooinoyo.star.badge.service.manager

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

class FloatingWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // 添加悬浮球
    fun addFloatingButton(view: View) {
        val params = createParams().apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
            y = 0
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            // FLAG_LAYOUT_NO_LIMITS: 允许延伸到状态栏/导航栏区域
            // FLAG_NOT_FOCUSABLE: 不抢键盘焦点
            // FLAG_WATCH_OUTSIDE_TOUCH: 允许监听外部点击（可选）
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }
        windowManager.addView(view, params)
    }

    // 添加菜单
    fun addMenu(view: View): WindowManager.LayoutParams {
        val params = createParams().apply {
            gravity = Gravity.END or Gravity.TOP
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            // 菜单需要允许穿透点击 (FLAG_NOT_TOUCH_MODAL) 以及覆盖整个屏幕高度 (FLAG_LAYOUT_NO_LIMITS)
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    fun removeView(view: View?) {
        if (view != null && view.isAttachedToWindow) {
            try {
                windowManager.removeView(view)
            } catch (e: IllegalArgumentException) {
                // Ignore if not attached
            }
        }
    }

    fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            // Ignore
        }
    }

    private fun createParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            0, 0, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }
}