package com.autocar.launcher.core.model.data

/**
 * 桌面部件 -自定义组件
 */
class WidgetItemInfo(
    var userThemePlugin: UserThemePlugin?= null
): ItemInfo(itemType = ItemType.WIDGET) {
    override fun toString(): String {
        return "WidgetItemInfo(userThemePlugin=$userThemePlugin) ${super.toString()}"
    }
}