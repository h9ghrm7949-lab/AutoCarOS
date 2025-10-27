package com.autocar.launcher.router

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.MainThread

/**
 * RouterNavigator：轻量页面导航封装
 * - 支持 class 跳转、uri deeplink、显式 Intent
 * - 提供 Builder 风格接口（部分借鉴 ARouter）
 *
 * 注意：Router 本身尽量保持 core-safe；Navigator 会依赖 Android API（应放在 ui 层或 router 模块的 android 源集）
 */

class Navigator private constructor() {
    data class Builder(
        var context: Context? = null,
        var target: Class<*>? = null,
        var uri: Uri? = null,
        val extras: Bundle = Bundle(),
        var flags: Int? = null
    ) {
        fun with(context: Context) = apply { this.context = context }
        fun to(clazz: Class<*>) = apply { this.target = clazz }
        fun to(uri: Uri) = apply { this.uri = uri }
        fun withBundle(bundle: Bundle) = apply { extras.putAll(bundle) }
        fun addFlag(flag: Int) = apply { flags = (flags ?: 0) or flag }

        @MainThread
        fun navigation() {
            val ctx = context ?: throw IllegalStateException("Navigator: context is required")
            when {
                target != null -> {
                    val intent = Intent(ctx, target).apply {
                        extras?.let {
                            putExtras(it)
                        }
                        addFlags(flags)
                    }
                    ctx.startActivity(intent)
                }
                uri != null -> {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        extras?.let {
                            putExtras(it)
                        }
                        addFlags(flags)
                        // context might be non-activity, so set FLAG_ACTIVITY_NEW_TASK
                        if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
                else -> throw IllegalStateException("Navigator: neither target nor uri specified")
            }
        }
    }

    companion object {
        @JvmStatic
        fun build(): Builder = Builder()
    }
}