package com.autocar.launcher.core.model.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户壁纸模块
 */
@Parcelize
data class ThemePluginWallpaper(
    var id: Int = 0,
    var cover: String = "", // 封面图片
    var path: String = "", // 图片路径
    var dynamic: Int = 0, // 自定义
    var isUsing: Boolean = false, // 是否正在使用
    var memorizedIsInitialized: Boolean = false, // 是否缓存啦
): Parcelable