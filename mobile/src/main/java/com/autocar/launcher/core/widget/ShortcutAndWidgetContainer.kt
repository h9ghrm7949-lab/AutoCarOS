package com.autocar.launcher.core.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.autocar.launcher.core.CellLayout
import kotlin.math.min

class ShortcutAndWidgetContainer(
    context: Context,
    attrs: AttributeSet?= null,
    defStyleAttr: Int = 0
): ViewGroup(context, attrs, defStyleAttr) {

    // 单格子宽度
    private var cellWidth: Int = 0
    // 单格子高度
    private var cellHeight: Int = 0
    // X轴有几个单元格
    private var countX: Int = 0
    // Y轴有几个单元格
    private var countY: Int = 0

    fun setCellDimensions(cellWidth: Int, cellHeight: Int, countX: Int, countY: Int) {
        this.cellWidth = cellWidth
        this.cellHeight = cellHeight
        this.countX = countX
        this.countY = countY
    }

    fun getChildAt(cellX: Int, cellY: Int): View? {
        val count = childCount
        for(i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as CellLayout.LayoutParams
            if ((lp.cellX <= cellX) && (cellX < lp.cellX + lp.cellHSpan)
                && (lp.cellY <= cellY) && (cellY < lp.cellY + lp.cellVSpan)) {
                return child;
            }
        }
        return null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = childCount
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSpecSize, heightSpecSize)
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChild(child)
            }
        }
    }

    fun measureChild(child: View) {
        val lp = child.layoutParams as CellLayout.LayoutParams
        lp.setup(cellWidth, cellHeight,1f, 1f)
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int
    ) {
        val count = childCount
        for(i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams
                layoutChild(child)
            }
        }
    }

    fun layoutChild(child: View) {
        val lp = child.layoutParams as CellLayout.LayoutParams
        if (child is WidgetCardView) {
            val widgetCardView = child
            widgetCardView.setScaleToFit(min(1.0f, 1.0f))
            widgetCardView.setTranslationForCentering(-(lp.width - (lp.width * scaleX)) / 2.0f,
                -(lp.height - (lp.height * scaleY)) / 2.0f)
        }
        val childLeft = lp.x
        val childTop = lp.y
        child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height)
    }

    fun setupLp(view: View) {
        val layoutParams = view.layoutParams as CellLayout.LayoutParams
        layoutParams.setup(cellWidth, cellHeight,1f,1f)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        if (child != null) {
            val r = Rect()
            child.getDrawingRect(r)
            requestRectangleOnScreen(r)
        }
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        val count = childCount
        for (i in 0 until count) {
            getChildAt(i).cancelLongPress()
        }
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

}