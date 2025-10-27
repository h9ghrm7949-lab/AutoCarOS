package com.autocar.launcher.core.util

import android.view.MotionEvent

/**
 * 触摸控制器，统一管理Launcher中不同场景的手势逻辑
 * 比如桌面滚动、编辑模式、拖拽、缩放等
 */
interface TouchController {

    /**
     * 是否消费触摸事件
     * return true 表示事件被处理
     */
    fun onControllerTouchEvent(ev: MotionEvent): Boolean

    /**
     * 是否拦截触摸事件
     * return true 表示拦截，不再下发给子view
     */
    fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean

}