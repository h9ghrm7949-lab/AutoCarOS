package com.autocar.launcher.core.util

data class GridConfig(
    var countX: Int = -1,
    var countY: Int = -1,
    var cellWidth: Int = 0,
    var cellHeight: Int = 0,
    var cellPadding: Int = 0,
    var borderRadius: Float = 0.0f
) {
    override fun toString(): String {
        return "GridConfig(countX=$countX, countY=$countY, cellWidth=$cellWidth, cellHeight=$cellHeight, cellPadding=$cellPadding, borderRadius=$borderRadius)"
    }
}