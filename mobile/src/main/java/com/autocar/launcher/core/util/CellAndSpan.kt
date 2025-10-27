package com.autocar.launcher.core.util

/**
 * 矩阵在网格单位的逻辑尺寸
 */
open class CellAndSpan(
    var cellX: Int = -1, // 桌面布局X坐标
    var cellY: Int = -1, // 桌面布局Y坐标
    var spanX: Int = 0, // 布局X轴上占据空间
    var spanY: Int = 0, // 布局Y轴上占用空间
) {
    fun copyFrom(copy: CellAndSpan) {
        this.cellX = copy.cellX
        this.cellY = copy.cellY
        this.spanX = copy.spanX
        this.spanY = copy.spanY
    }

    override fun toString(): String {
        return "CellAndSpan(cellX=$cellX, cellY=$cellY, spanX=$spanX, spanY=$spanY)"
    }


}