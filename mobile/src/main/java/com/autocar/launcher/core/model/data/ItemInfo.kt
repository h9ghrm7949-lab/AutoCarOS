package com.autocar.launcher.core.model.data

import android.content.ComponentName
import androidx.annotation.IntDef

open class ItemInfo(
    @ItemType
    var itemType: Int = ItemType.APP, // 类型
    var cellX: Int = -1, // 桌面布局X坐标
    var cellY: Int = -1, // 桌面布局Y坐标
    var spanX: Int = 0, // 布局X轴上占据空间
    var spanY: Int = 0, // 布局Y轴上占用空间
    var minSpanX: Int = 0, // 预留-布局在X轴上最小空间
    var minSpanY: Int = 0, // 预留-布局在Y轴上最小空间
    var title: String = "", // 标题
    var contentDescription: String = "", // 描述
    var componentName: ComponentName ?= null
) {

    @IntDef(ItemType.APP, ItemType.WIDGET)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ItemType {
        companion object {
            const val APP = 0
            const val WIDGET = 1
        }
    }

    override fun toString(): String {
        return "ItemInfo(itemType=$itemType, cellX=$cellX, cellY=$cellY, spanX=$spanX, spanY=$spanY, minSpanX=$minSpanX, minSpanY=$minSpanY, title='$title', contentDescription='$contentDescription', componentName=$componentName)"
    }


}