package com.autocar.launcher.core.model.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户主题插件信息
 * 用于描述一个自定义Widget的外观、布局、跳转行为等信息
 */
@Parcelize
data class UserThemePlugin(
    var id: Int = 0,
    var themeId: String = "", // 主题资源名 比如 "theme_night"
    var themeName: String = "", // 主题名称（例如"夜间模式", "白天模式"）
    var packageName: String = "", // 主题包名 比如"com.autocar.theme.night"
    var borderRadius: String = "", // 远郊半径（单位：DP）
    var cellX: String = "", // 桌面布局X坐标
    var cellY: String = "", // 桌面布局Y坐标
    var spanX: String = "", // 布局X轴上占据空间
    var spanY: String = "", // 布局Y轴上占用空间
    var disableDelete: Boolean = false, // 是否禁止删除
    var disableMove: Boolean = false, // 是否禁止拖动
    var jumpType: Int = 0, // 跳转类型，比如 0 无跳转 1 打开内部页面 2 启动外部APP 3 跳转到URL
    var jumpUrl: String = "", // 跳转目标或URI，比如"https://..." 或者"autocar://music"
    var entryClass: String ?= null, // 插件View入口类，如"com.autocar.theme.NightWidgetView"
    var configPath: String ?= null, // 可选 插件资源配置文件路径
): Parcelable {
    override fun toString(): String {
        return "UserThemePlugin(id=$id, themeId='$themeId', themeName='$themeName', packageName='$packageName', borderRadius='$borderRadius', cellX='$cellX', cellY='$cellY', spanX='$spanX', spanY='$spanY', disableDelete=$disableDelete, disableMove=$disableMove, jumpType=$jumpType, jumpUrl='$jumpUrl', entryClass=$entryClass, configPath=$configPath)"
    }
}