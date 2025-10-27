package com.autocar.launcher.core.util

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * 显示工具类，用于处理与屏幕尺寸、密度、单位转换相关的通用方法。
 * 在桌面（Launcher）中，常用于计算图标大小、间距、动画偏移等。
 */
object DisplayUtil {

    /**
     * 将 dp 转换为 px
     */
    fun dpToPx(context: Context, dp: Float): Int {
        val metrics = context.resources.displayMetrics
        return (dp * metrics.density + 0.5f).toInt()
    }

    /**
     * 将 px 转换为 dp
     */
    fun pxToDp(context: Context, px: Float): Float {
        val metrics = context.resources.displayMetrics
        return px / metrics.density
    }

    /**
     * 将 sp 转换为 px（通常用于文字大小）
     */
    fun spToPx(context: Context, sp: Float): Int {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics).toInt()
    }

    /**
     * 获取屏幕宽度（像素）
     */
    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * 获取屏幕高度（像素）
     */
    fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * 获取屏幕密度（如 2.0、3.0）
     */
    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    /**
     * 获取屏幕 DPI（每英寸像素点数）
     */
    fun getDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * 获取系统默认的图标大小（Launcher常用）
     * @param context 上下文
     * @param defaultDp 默认大小（dp），例如 48dp
     */
    fun getIconSizePx(context: Context, defaultDp: Float = 48f): Int {
        return dpToPx(context, defaultDp)
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    /**
     * 获取导航栏高度（部分设备可能为0）
     */
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    /**
     * 获取 DisplayMetrics（屏幕信息对象）
     */
    fun getDisplayMetrics(): DisplayMetrics {
        return Resources.getSystem().displayMetrics
    }
}