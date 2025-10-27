package com.autocar.launcher.core.util

import android.graphics.Rect
import com.autocar.launcher.core.model.data.ItemInfo

/**
 * Utility object to manage the occupancy in a grid.
 */
class GridOccupancy(private val mCountX: Int, private val mCountY: Int) {
    val cells: Array<BooleanArray> = Array(mCountX) { BooleanArray(mCountY) }

    /**
     * Find the first vacant cell, if there is one.
     *
     * @param vacantOut Holds the x and y coordinate of the vacant cell
     * @param spanX Horizontal cell span.
     * @param spanY Vertical cell span.
     *
     * @return true if a vacant cell was found
     */
    fun findVacantCell(vacantOut: IntArray, spanX: Int, spanY: Int): Boolean {
        var y = 0
        while ((y + spanY) <= mCountY) {
            var x = 0
            while ((x + spanX) <= mCountX) {
                var available = !cells[x]!![y]
                out@ for (i in x..<x + spanX) {
                    for (j in y..<y + spanY) {
                        available = available && !cells[i]!![j]
                        if (!available) break@out
                    }
                }
                if (available) {
                    vacantOut[0] = x
                    vacantOut[1] = y
                    return true
                }
                x++
            }
            y++
        }
        return false
    }

    fun copyTo(dest: GridOccupancy) {
        for (i in 0..<mCountX) {
            for (j in 0..<mCountY) {
                dest.cells[i]!![j] = cells[i]!![j]
            }
        }
    }

    fun isRegionVacant(x: Int, y: Int, spanX: Int, spanY: Int): Boolean {
        val x2 = x + spanX - 1
        val y2 = y + spanY - 1
        if (x < 0 || y < 0 || x2 >= mCountX || y2 >= mCountY) {
            return false
        }
        for (i in x..x2) {
            for (j in y..y2) {
                if (cells[i]!![j]) {
                    return false
                }
            }
        }
        return true
    }

    fun markCells(cellX: Int, cellY: Int, spanX: Int, spanY: Int, value: Boolean) {
        if (cellX < 0 || cellY < 0) return
        var x = cellX
        while (x < cellX + spanX && x < mCountX) {
            var y = cellY
            while (y < cellY + spanY && y < mCountY) {
                cells[x]!![y] = value
                y++
            }
            x++
        }
    }

    fun markCells(r: Rect, value: Boolean) {
        markCells(r.left, r.top, r.width(), r.height(), value)
    }

    fun markCells(cell: CellAndSpan, value: Boolean) {
        markCells(cell.cellX, cell.cellY, cell.spanX, cell.spanY, value)
    }

    fun markCells(item: ItemInfo, value: Boolean) {
        markCells(item.cellX, item.cellY, item.spanX, item.spanY, value)
    }

    fun clear() {
        markCells(0, 0, mCountX, mCountY, false)
    }
}
