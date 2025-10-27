package com.autocar.launcher.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.ArrayMap
import android.util.AttributeSet
import android.util.Property
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.view.isNotEmpty
import androidx.core.view.size
import com.android.app.animation.InterpolatorsAndroidX
import com.autocar.launcher.R
import com.autocar.launcher.core.ViewClusterConfig.BOTTOM
import com.autocar.launcher.core.ViewClusterConfig.LEFT
import com.autocar.launcher.core.ViewClusterConfig.RIGHT
import com.autocar.launcher.core.ViewClusterConfig.TOP
import com.autocar.launcher.core.anim.Interpolators
import com.autocar.launcher.core.anim.InterruptibleInOutAnimator
import com.autocar.launcher.core.constant.DeviceProfile
import com.autocar.launcher.core.model.data.ItemInfo
import com.autocar.launcher.core.reorder.Reorderable
import com.autocar.launcher.core.util.CellAndSpan
import com.autocar.launcher.core.util.DisplayUtil
import com.autocar.launcher.core.util.Flags.areAnimationsEnabled
import com.autocar.launcher.core.util.GridOccupancy
import com.autocar.launcher.core.util.ParcelableSparseArray
import com.autocar.launcher.core.widget.ShortcutAndWidgetContainer
import timber.log.Timber
import java.lang.Float.compare
import java.util.Arrays
import java.util.Collections
import java.util.Stack
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    private var cellWidth: Int

    private var cellHeight: Int
    private var mFixedCellWidth: Int
    private var mFixedCellHeight: Int

    private var mBorderSpacing: Int

    var countX: Int
        get() = countX


    var countY: Int
        get() = countX

    var isDropPending: Boolean = false

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private val mTmpPoint: IntArray = IntArray(2)

    private val mTempLocation: IntArray = IntArray(2)
    private val mTmpPointF: PointF = PointF()

    private val mVisualizeGridRect = Rect()
    private val mVisualizeGridPaint = Paint()

    private var mOccupied: GridOccupancy
    private var mTmpOccupied: GridOccupancy

    private var mInterceptTouchListener: OnTouchListener? = null

    private val mDelegatedCellDrawings: ArrayList<DelegatedCellDrawing?> = ArrayList()

    val scrimBackground: Drawable

    // These values allow a fixed measurement to be set on the CellLayout.
    private var mFixedWidth = -1
    private var mFixedHeight = -1

    // If we're actively dragging something over this screen, mIsDragOverlapping is true
    private var mIsDragOverlapping = false


    private val mDragOutlines= arrayOfNulls<Rect>(4)
    private val mDragOutlineAlphas: FloatArray = FloatArray(mDragOutlines.size)
    private val mDragOutlineAnims = arrayOfNulls<InterruptibleInOutAnimator>(mDragOutlines.size)

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    private var mDragOutlineCurrent = 0
    private val mDragOutlinePaint = Paint()

    private val mReorderAnimators: ArrayMap<LayoutParams?, Animator?> = ArrayMap<LayoutParams?, Animator?>()

    private val mShakeAnimators = ArrayMap<Reorderable, ReorderPreviewAnimation>()
    private var isItemPlacementDirty: Boolean = false

    // When a drag operation is in progress, holds the nearest cell to the touch point
    private val mDragCell = IntArray(2)

    private var mDragging = false

    private val mEaseOutInterpolator: TimeInterpolator
    private val mShortcutsAndWidgets: ShortcutAndWidgetContainer

    private val mChildScale = 1f

    private val mReorderPreviewAnimationMagnitude: Float

    private val mIntersectingViews: ArrayList<View> = ArrayList()
    private val mOccupiedRect = Rect()
    private val mDirectionVector = IntArray(2)
    val mPreviousReorderDirection: IntArray = IntArray(2)
    private val mTempRect = Rect()



    fun enableHardwareLayer(hasLayer: Boolean) {
        mShortcutsAndWidgets.setLayerType(
            if (hasLayer) LAYER_TYPE_HARDWARE else LAYER_TYPE_NONE,
            sPaint
        )
    }

    val isHardwareLayerEnabled: Boolean
        get() = mShortcutsAndWidgets.layerType == LAYER_TYPE_HARDWARE

    fun setCellDimensions(width: Int, height: Int) {
        this.cellWidth = width
        mFixedCellWidth = this.cellWidth
        this.cellHeight = height
        mFixedCellHeight = this.cellHeight
        mShortcutsAndWidgets.setCellDimensions(
            this.cellWidth,
            this.cellHeight,
            this.countX,
            this.countY
        )
    }

    fun setGridSize(x: Int, y: Int) {
        this.countX = x
        this.countY = y
        mOccupied = GridOccupancy(this.countX, this.countY)
        mTmpOccupied = GridOccupancy(this.countX, this.countY)
        mTempRectStack.clear()
        mShortcutsAndWidgets.setCellDimensions(
            this.cellWidth,
            this.cellHeight,
            this.countX,
            this.countY
        )
        requestLayout()
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        val jail: ParcelableSparseArray = getJailedArray(container)
        super.dispatchSaveInstanceState(jail)
        container.put(R.id.cell_layout_jail_id, jail)
    }




    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchRestoreInstanceState(getJailedArray(container))
    }

    /**
     * Wrap the SparseArray in another Parcelable so that the item ids do not conflict with our
     * our internal resource ids
     */
    private fun getJailedArray(container: SparseArray<Parcelable>): ParcelableSparseArray {
        val parcelable: Parcelable? = container.get(R.id.cell_layout_jail_id)
        return parcelable as? ParcelableSparseArray ?: ParcelableSparseArray()
    }

    var isDragOverlapping: Boolean
        get() = mIsDragOverlapping
        set(isDragOverlapping) {
            if (mIsDragOverlapping != isDragOverlapping) {
                mIsDragOverlapping = isDragOverlapping
                scrimBackground.setState(
                    (if (mIsDragOverlapping)
                        BACKGROUND_STATE_ACTIVE
                    else
                        BACKGROUND_STATE_DEFAULT)!!
                )
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        // When we're large, we are either drawn in a "hover" state (ie when dragging an item to
        // a neighboring page) or with just a normal background (if backgroundAlpha > 0.0f)
        // When we're small, we are either drawn normally or in the "accepts drops" state (during
        // a drag). However, we also drag the mini hover background *over* one of those two
        // backgrounds
        if (scrimBackground.alpha > 0) {
            scrimBackground.draw(canvas)
        }

        val paint = mDragOutlinePaint
        for (i in mDragOutlines.indices) {
            val alpha = mDragOutlineAlphas[i]
            if (alpha > 0) {
                val b: Bitmap = mDragOutlineAnims[i]!!.tag as Bitmap
                paint.setAlpha((alpha + .5f).toInt())
                canvas.drawBitmap(b, null, mDragOutlines[i]!!, paint)
            }
        }

        for (i in 0..<mDelegatedCellDrawings.size) {
            val cellDrawing: DelegatedCellDrawing = mDelegatedCellDrawings.get(i)!!
            cellToPoint(cellDrawing.mDelegateCellX, cellDrawing.mDelegateCellY, mTempLocation)
            canvas.save()
            canvas.translate(mTempLocation[0].toFloat(), mTempLocation[1].toFloat())
            cellDrawing.drawUnderItem(canvas)
            canvas.restore()
        }

        if (VISUALIZE_GRID) {
            visualizeGrid(canvas)
        }
    }

    fun visualizeGrid(canvas: Canvas) {
        mVisualizeGridRect.set(0, 0, this.cellWidth, this.cellHeight)
        mVisualizeGridPaint.strokeWidth = 4f

        for (i in 0..<this.countX) {
            for (j in 0..<this.countY) {
                canvas.withSave {

                    val transX = i * cellWidth
                    val transY = j * cellHeight

                    translate(
                        (getPaddingLeft() + transX).toFloat(),
                        (paddingTop + transY).toFloat()
                    )

                    mVisualizeGridPaint.style = Paint.Style.FILL
                    mVisualizeGridPaint.setColor(Color.argb(80, 255, 100, 100))

                    drawRect(mVisualizeGridRect, mVisualizeGridPaint)

                    mVisualizeGridPaint.style = Paint.Style.STROKE
                    mVisualizeGridPaint.setColor(Color.argb(255, 255, 100, 100))

                    drawRect(mVisualizeGridRect, mVisualizeGridPaint)
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        for (i in 0..<mDelegatedCellDrawings.size) {
            val bg: DelegatedCellDrawing = mDelegatedCellDrawings.get(i)!!
            cellToPoint(bg.mDelegateCellX, bg.mDelegateCellY, mTempLocation)
            canvas.withTranslation(mTempLocation[0].toFloat(), mTempLocation[1].toFloat()) {
                bg.drawOverItem(this)
            }
        }
    }

    /**
     * Add Delegated cell drawing
     */
    fun addDelegatedCellDrawing(bg: DelegatedCellDrawing) {
        mDelegatedCellDrawings.add(bg)
    }

    /**
     * Remove item from DelegatedCellDrawings
     */
    fun removeDelegatedCellDrawing(bg: DelegatedCellDrawing?) {
        mDelegatedCellDrawings.remove(bg)
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

    fun restoreInstanceState(states: SparseArray<Parcelable>) {
        try {
            dispatchRestoreInstanceState(states)
        } catch (ex: IllegalArgumentException) {
            // Mismatched viewId / viewType preventing restore. Skip restore on production builds.
            Timber.tag(TAG).e(ex, "Ignoring an error while restoring a view instance state")
        }
    }

    override fun cancelLongPress() {
        super.cancelLongPress()

        // Cancel long press for all children
        for (i in 0..<size) {
            val child = getChildAt(i)
            child.cancelLongPress()
        }
    }

    fun setOnInterceptTouchListener(listener: OnTouchListener?) {
        mInterceptTouchListener = listener
    }

    fun acceptsWidget(): Boolean {
        return true
    }

    fun addViewToCellLayout(
        child: View, index: Int, childId: Int, params: LayoutParams,
        markCells: Boolean
    ): Boolean {
        val lp = params
        child.scaleX = mChildScale
        child.scaleY = mChildScale

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= this.countX - 1 && lp.cellY >= 0 && lp.cellY <= this.countY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = this.countX
            if (lp.cellVSpan < 0) lp.cellVSpan = this.countY

            child.setId(childId)
            if (LOGD) {
                Timber.tag(TAG).d("Adding view to ShortcutsAndWidgetsContainer: " + child)
            }
            mShortcutsAndWidgets.addView(child, index, lp)

            if (markCells) markCellsAsOccupiedForView(child)

            return true
        }
        return false
    }

    override fun removeAllViews() {
        mOccupied.clear()
        mShortcutsAndWidgets.removeAllViews()
    }

    override fun removeAllViewsInLayout() {
        if (mShortcutsAndWidgets.isNotEmpty()) {
            mOccupied.clear()
            mShortcutsAndWidgets.removeAllViewsInLayout()
        }
    }

    override fun removeView(view: View?) {
        markCellsAsUnoccupiedForView(view)
        mShortcutsAndWidgets.removeView(view)
    }

    override fun removeViewAt(index: Int) {
        markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(index))
        mShortcutsAndWidgets.removeViewAt(index)
    }

    override fun removeViewInLayout(view: View?) {
        markCellsAsUnoccupiedForView(view)
        mShortcutsAndWidgets.removeViewInLayout(view)
    }

    override fun removeViews(start: Int, count: Int) {
        for (i in start..<start + count) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i))
        }
        mShortcutsAndWidgets.removeViews(start, count)
    }

    override fun removeViewsInLayout(start: Int, count: Int) {
        for (i in start..<start + count) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i))
        }
        mShortcutsAndWidgets.removeViewsInLayout(start, count)
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    fun pointToCellExact(x: Int, y: Int, result: IntArray) {
        val hStartPadding = getPaddingLeft()
        val vStartPadding = getPaddingTop()

        result[0] = (x - hStartPadding) / this.cellWidth
        result[1] = (y - vStartPadding) / this.cellHeight

        val xAxis = this.countX
        val yAxis = this.countY

        if (result[0] < 0) result[0] = 0
        if (result[0] >= xAxis) result[0] = xAxis - 1
        if (result[1] < 0) result[1] = 0
        if (result[1] >= yAxis) result[1] = yAxis - 1
    }

    /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    fun pointToCellRounded(x: Int, y: Int, result: IntArray) {
        pointToCellExact(x + (this.cellWidth / 2), y + (this.cellHeight / 2), result)
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    fun cellToPoint(cellX: Int, cellY: Int, result: IntArray) {
        val hStartPadding = getPaddingLeft()
        val vStartPadding = getPaddingTop()

        result[0] = hStartPadding + cellX * this.cellWidth
        result[1] = vStartPadding + cellY * this.cellHeight
    }

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    fun cellToCenterPoint(cellX: Int, cellY: Int, result: IntArray) {
        regionToCenterPoint(cellX, cellY, 1, 1, result)
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the regio
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    fun regionToCenterPoint(cellX: Int, cellY: Int, spanX: Int, spanY: Int, result: IntArray) {
        val hStartPadding = getPaddingLeft()
        val vStartPadding = getPaddingTop()
        result[0] = hStartPadding + cellX * this.cellWidth + (spanX * this.cellWidth) / 2
        result[1] = vStartPadding + cellY * this.cellHeight + (spanY * this.cellHeight) / 2
    }

    /**
     * Given a cell coordinate and span fills out a corresponding pixel rect
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     * @param result Rect in which to write the result
     */
    fun regionToRect(cellX: Int, cellY: Int, spanX: Int, spanY: Int, result: Rect) {
        val hStartPadding = getPaddingLeft()
        val vStartPadding = getPaddingTop()
        val left = hStartPadding + cellX * this.cellWidth
        val top = vStartPadding + cellY * this.cellHeight
        result.set(left, top, left + (spanX * this.cellWidth), top + (spanY * this.cellHeight))
    }

    fun getDistanceFromCell(x: Float, y: Float, cell: IntArray): Float {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint)
        return hypot((x - mTmpPoint[0]).toDouble(), (y - mTmpPoint[1]).toDouble()).toFloat()
    }

    fun setFixedSize(width: Int, height: Int) {
        mFixedWidth = width
        mFixedHeight = height
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight())
        val childHeightSize = heightSize - (paddingTop + paddingBottom)

        if (mFixedCellWidth < 0 || mFixedCellHeight < 0) {
            val ch: Int = calculateCellHeight(
                childHeightSize,
                mBorderSpacing,
                this.countY
            )
            val cw = ch
            if (cw != this.cellWidth || ch != this.cellHeight) {
                this.cellWidth = cw
                this.cellHeight = ch
                mShortcutsAndWidgets.setCellDimensions(
                    this.cellWidth,
                    this.cellHeight,
                    this.countX,
                    this.countY
                )
            }
        }

        var newWidth = childWidthSize
        var newHeight = childHeightSize
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            newWidth = mFixedWidth
            newHeight = mFixedHeight
        } else if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw RuntimeException("CellLayout cannot have UNSPECIFIED dimensions")
        }

        mShortcutsAndWidgets.measure(
            MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
        )

        val maxWidth: Int = mShortcutsAndWidgets.getMeasuredWidth()
        val maxHeight: Int = mShortcutsAndWidgets.getMeasuredHeight()
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            setMeasuredDimension(maxWidth, maxHeight)
        } else {
            setMeasuredDimension(widthSize, heightSize)
        }
    }

    fun calculateCellWidth(width: Int, borderSpacing: Int, countX: Int): Int {
        return (width - ((countX - 1) * borderSpacing)) / countX
    }

    fun calculateCellHeight(height: Int, borderSpacing: Int, countY: Int): Int {
        return (height - ((countY - 1) * borderSpacing)) / countY
    }



    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = getPaddingLeft()
        left += ceil((this.unusedHorizontalSpace / 2f).toDouble()).toInt()
        var right = r - l - getPaddingRight()
        right -= ceil((this.unusedHorizontalSpace / 2f).toDouble()).toInt()

        val top = getPaddingTop()
        val bottom = b - t - getPaddingBottom()

        // Expand the background drawing bounds by the padding baked into the background drawable
        scrimBackground.getPadding(mTempRect)
        scrimBackground.setBounds(
            left - mTempRect.left - getPaddingLeft(),
            top - mTempRect.top - getPaddingTop(),
            right + mTempRect.right + getPaddingRight(),
            bottom + mTempRect.bottom + getPaddingBottom()
        )

        mShortcutsAndWidgets.layout(left, top, right, bottom)
    }

    val unusedHorizontalSpace: Int
        /**
         * Returns the amount of space left over after subtracting padding and cells. This space will be
         * very small, a few pixels at most, and is a result of rounding down when calculating the cell
         * width in [DeviceProfile.calculateCellWidth].
         */
        get() = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - (this.countX * this.cellWidth)

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || (who === this.scrimBackground)
    }

    val shortcutsAndWidgets: ShortcutAndWidgetContainer
        get() = mShortcutsAndWidgets

    fun getChildAt(x: Int, y: Int): View? {
        return mShortcutsAndWidgets.getChildAt(x, y)
    }

    fun animateChildToPosition(
        child: View?, cellX: Int, cellY: Int, duration: Int,
        delay: Int, permanent: Boolean, adjustOccupied: Boolean
    ): Boolean {
        val clc: ShortcutAndWidgetContainer = this.shortcutsAndWidgets

        if (clc.indexOfChild(child) != -1 && (child is Reorderable)) {
            val lp = child.layoutParams as LayoutParams
            val info = child.tag as ItemInfo
            val item: Reorderable = child as Reorderable

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp)!!.cancel()
                mReorderAnimators.remove(lp)
            }


            if (adjustOccupied) {
                val occupied: GridOccupancy = if (permanent) mOccupied else mTmpOccupied
                occupied.markCells(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, false)
                occupied.markCells(cellX, cellY, lp.cellHSpan, lp.cellVSpan, true)
            }

            // Compute the new x and y position based on the new cellX and cellY
            // We leverage the actual layout logic in the layout params and hence need to modify
            // state and revert that state.
            val oldX = lp.x
            val oldY = lp.y
            lp.isLockedToGrid = true
            if (permanent) {
                info.cellX = cellX
                lp.cellX = info.cellX
                info.cellY = cellY
                lp.cellY = info.cellY
            } else {
                lp.tmpCellX = cellX
                lp.tmpCellY = cellY
            }
            clc.setupLp(child)
            val newX = lp.x
            val newY = lp.y
            lp.x = oldX
            lp.y = oldY
            lp.isLockedToGrid = false

            // End compute new x and y
            item.getReorderPreviewOffset(mTmpPointF)
            val initPreviewOffsetX = mTmpPointF.x
            val initPreviewOffsetY = mTmpPointF.y
            val finalPreviewOffsetX = (newX - oldX).toFloat()
            val finalPreviewOffsetY = (newY - oldY).toFloat()


            // Exit early if we're not actually moving the view
            if (finalPreviewOffsetX == 0f && finalPreviewOffsetY == 0f && initPreviewOffsetX == 0f && initPreviewOffsetY == 0f) {
                lp.isLockedToGrid = true
                return true
            }

            val va: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
            va.setDuration(duration.toLong())
            mReorderAnimators.put(lp, va)

            va.addUpdateListener { animation ->
                val r = animation.getAnimatedValue() as Float
                val x = (1 - r) * initPreviewOffsetX + r * finalPreviewOffsetX
                val y = (1 - r) * initPreviewOffsetY + r * finalPreviewOffsetY
                item.setReorderPreviewOffset(x, y)
            }
            va.addListener(object : AnimatorListenerAdapter() {
                var cancelled: Boolean = false
                override fun onAnimationEnd(animation: Animator) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true
                        item.setReorderPreviewOffset(0f, 0f)
                        child.requestLayout()
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }
            })
            va.setStartDelay(delay.toLong())
            va.start()
            return true
        }
        return false
    }


    @SuppressLint("StringFormatMatches")
    fun getItemMoveDescription(cellX: Int, cellY: Int): String {
        return context.getString(
            R.string.move_to_empty_cell,
            cellY + 1, cellX + 1
        )
    }


    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     * be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    fun findNearestVacantArea(
        pixelX: Int, pixelY: Int, minSpanX: Int, minSpanY: Int, spanX: Int,
        spanY: Int, result: IntArray?, resultSpan: IntArray?
    ): IntArray {
        return findNearestArea(
            pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, true,
            result, resultSpan
        )
    }

    private val mTempRectStack = Stack<Rect>()
    private fun lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (i in 0..<this.countX * this.countY) {
                mTempRectStack.push(Rect())
            }
        }
    }

    private fun recycleTempRects(used: Stack<Rect>) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop())
        }
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     * be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    private fun findNearestArea(
        pixelX: Int, pixelY: Int, minSpanX: Int, minSpanY: Int, spanX: Int,
        spanY: Int, ignoreOccupied: Boolean, result: IntArray?, resultSpan: IntArray?
    ): IntArray {
        var pixelX = pixelX
        var pixelY = pixelY
        lazyInitTempRectStack()

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX = (pixelX - this.cellWidth * (spanX - 1) / 2f).toInt()
        pixelY = (pixelY - this.cellHeight * (spanY - 1) / 2f).toInt()

        // Keep track of best-scoring drop area
        val bestXY: IntArray? = if (result != null) result else IntArray(2)
        var bestDistance = Double.Companion.MAX_VALUE
        val bestRect = Rect(-1, -1, -1, -1)
        val validRegions = Stack<Rect>()

        val countX = this.countX
        val countY = this.countY

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 || spanX < minSpanX || spanY < minSpanY) {
            return bestXY!!
        }

        for (y in 0..<countY - (minSpanY - 1)) {
            inner@ for (x in 0..<countX - (minSpanX - 1)) {
                var ySize = -1
                var xSize = -1
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (i in 0..<minSpanX) {
                        for (j in 0..<minSpanY) {
                            if (mOccupied.cells[x + i][y + j]) {
                                continue@inner
                            }
                        }
                    }
                    xSize = minSpanX
                    ySize = minSpanY

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    var incX = true
                    var hitMaxX = xSize >= spanX
                    var hitMaxY = ySize >= spanY
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (j in 0..<ySize) {
                                if (x + xSize > countX - 1 || mOccupied.cells[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true
                                }
                            }
                            if (!hitMaxX) {
                                xSize++
                            }
                        } else if (!hitMaxY) {
                            for (i in 0..<xSize) {
                                if (y + ySize > countY - 1 || mOccupied.cells[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true
                                }
                            }
                            if (!hitMaxY) {
                                ySize++
                            }
                        }
                        hitMaxX = hitMaxX or (xSize >= spanX)
                        hitMaxY = hitMaxY or (ySize >= spanY)
                        incX = !incX
                    }
                    incX = true
                    hitMaxX = xSize >= spanX
                    hitMaxY = ySize >= spanY
                }
                val cellXY = mTmpPoint
                cellToCenterPoint(x, y, cellXY)

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                val currentRect = mTempRectStack.pop()
                currentRect.set(x, y, x + xSize, y + ySize)
                var contained = false
                for (r in validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true
                        break
                    }
                }
                validRegions.push(currentRect)
                val distance =
                    hypot((cellXY[0] - pixelX).toDouble(), (cellXY[1] - pixelY).toDouble())

                if ((distance <= bestDistance && !contained) ||
                    currentRect.contains(bestRect)
                ) {
                    bestDistance = distance
                    bestXY!![0] = x
                    bestXY[1] = y
                    if (resultSpan != null) {
                        resultSpan[0] = xSize
                        resultSpan[1] = ySize
                    }
                    bestRect.set(currentRect)
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.Companion.MAX_VALUE) {
            bestXY!![0] = -1
            bestXY[1] = -1
        }
        recycleTempRects(validRegions)
        return bestXY!!
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location, and will also weigh in a suggested direction vector of the
     * desired location. This method computers distance based on unit grid distances,
     * not pixel distances.
     *
     * @param cellX The X cell nearest to which you want to search for a vacant area.
     * @param cellY The Y cell nearest which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param direction The favored direction in which the views should move from x, y
     * @param occupied The array which represents which cells in the CellLayout are occupied
     * @param blockOccupied The array which represents which cells in the specified block (cellX,
     * cellY, spanX, spanY) are occupied. This is used when try to move a group of views.
     * @param result Array in which to place the result, or null (in which case a new array will
     * be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    private fun findNearestArea(
        cellX: Int, cellY: Int, spanX: Int, spanY: Int, direction: IntArray,
        occupied: Array<BooleanArray>, blockOccupied: Array<BooleanArray>?, result: IntArray?
    ): IntArray {
        // Keep track of best-scoring drop area
        val bestXY: IntArray? = if (result != null) result else IntArray(2)
        var bestDistance = Float.Companion.MAX_VALUE
        var bestDirectionScore = Int.Companion.MIN_VALUE

        val countX = this.countX
        val countY = this.countY

        for (y in 0..<countY - (spanY - 1)) {
            inner@ for (x in 0..<countX - (spanX - 1)) {
                // First, let's see if this thing fits anywhere
                for (i in 0..<spanX) {
                    for (j in 0..<spanY) {
                        if (occupied[x + i]!![y + j] && (blockOccupied == null || blockOccupied[i]!![j])) {
                            continue@inner
                        }
                    }
                }

                val distance = hypot((x - cellX).toDouble(), (y - cellY).toDouble()).toFloat()
                val curDirection = mTmpPoint
                computeDirectionVector((x - cellX).toFloat(), (y - cellY).toFloat(), curDirection)
                // The direction score is just the dot product of the two candidate direction
                // and that passed in.
                val curDirectionScore = direction[0] * curDirection[0] +
                        direction[1] * curDirection[1]
                if (compare(distance, bestDistance) < 0 ||
                    (compare(distance, bestDistance) == 0
                            && curDirectionScore > bestDirectionScore)
                ) {
                    bestDistance = distance
                    bestDirectionScore = curDirectionScore
                    bestXY!![0] = x
                    bestXY[1] = y
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == kotlin.Float.Companion.MAX_VALUE) {
            bestXY!![0] = -1
            bestXY[1] = -1
        }
        return bestXY!!
    }

    private fun addViewToTempLocation(
        v: View?, rectOccupiedByPotentialDrop: Rect,
        direction: IntArray, currentState: ItemConfiguration
    ): Boolean {
        val c: CellAndSpan = currentState.map.get(v)!!
        var success = false
        mTmpOccupied.markCells(c, false)
        mTmpOccupied.markCells(rectOccupiedByPotentialDrop, true)

        findNearestArea(
            c.cellX, c.cellY, c.spanX, c.spanY, direction,
            mTmpOccupied.cells, null, mTempLocation
        )

        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            c.cellX = mTempLocation[0]
            c.cellY = mTempLocation[1]
            success = true
        }
        mTmpOccupied.markCells(c, true)
        return success
    }

    /**
     * This helper class defines a cluster of views. It helps with defining complex edges
     * of the cluster and determining how those edges interact with other views. The edges
     * essentially define a fine-grained boundary around the cluster of views -- like a more
     * precise version of a bounding box.
     */
    private inner class ViewCluster(views: ArrayList<View>, val config: ItemConfiguration) {
        val views: ArrayList<View> = views.clone() as ArrayList<View>
        val boundingRect: Rect = Rect()

        val leftEdge: IntArray = IntArray(countX)
        val rightEdge: IntArray = IntArray(countY)
        val topEdge: IntArray = IntArray(countX)
        val bottomEdge: IntArray = IntArray(countX)
        var dirtyEdges: Int = 0
        var boundingRectDirty: Boolean = false

        fun resetEdges() {
            for (i in 0..< countX) {
                topEdge[i] = -1
                bottomEdge[i] = -1
            }
            for (i in 0..< countY) {
                leftEdge[i] = -1
                rightEdge[i] = -1
            }
            dirtyEdges = LEFT or TOP or RIGHT or BOTTOM
            boundingRectDirty = true
        }

        fun computeEdge(which: Int) {
            val count: Int = views.size
            for (i in 0..<count) {
                val cs: CellAndSpan = config.map.get(views.get(i))!!
                when (which) {
                    LEFT -> {
                        val left: Int = cs.cellX
                        var j: Int = cs.cellY
                        while (j < cs.cellY + cs.spanY) {
                            if (left < leftEdge[j] || leftEdge[j] < 0) {
                                leftEdge[j] = left
                            }
                            j++
                        }
                    }

                    RIGHT -> {
                        val right: Int = cs.cellX + cs.spanX
                        var j: Int = cs.cellY
                        while (j < cs.cellY + cs.spanY) {
                            if (right > rightEdge[j]) {
                                rightEdge[j] = right
                            }
                            j++
                        }
                    }

                    TOP -> {
                        val top: Int = cs.cellY
                        var j: Int = cs.cellX
                        while (j < cs.cellX + cs.spanX) {
                            if (top < topEdge[j] || topEdge[j] < 0) {
                                topEdge[j] = top
                            }
                            j++
                        }
                    }

                    BOTTOM -> {
                        val bottom: Int = cs.cellY + cs.spanY
                        var j: Int = cs.cellX
                        while (j < cs.cellX + cs.spanX) {
                            if (bottom > bottomEdge[j]) {
                                bottomEdge[j] = bottom
                            }
                            j++
                        }
                    }
                }
            }
        }

        fun isViewTouchingEdge(v: View?, whichEdge: Int): Boolean {
            val cs: CellAndSpan = config.map.get(v)!!

            if ((dirtyEdges and whichEdge) == whichEdge) {
                computeEdge(whichEdge)
                dirtyEdges = dirtyEdges and whichEdge.inv()
            }

            when (whichEdge) {
                LEFT -> {
                    var i: Int = cs.cellY
                    while (i < cs.cellY + cs.spanY) {
                        if (leftEdge[i] == cs.cellX + cs.spanX) {
                            return true
                        }
                        i++
                    }
                }

                RIGHT -> {
                    var i: Int = cs.cellY
                    while (i < cs.cellY + cs.spanY) {
                        if (rightEdge[i] == cs.cellX) {
                            return true
                        }
                        i++
                    }
                }

                TOP -> {
                    var i: Int = cs.cellX
                    while (i < cs.cellX + cs.spanX) {
                        if (topEdge[i] == cs.cellY + cs.spanY) {
                            return true
                        }
                        i++
                    }
                }

                BOTTOM -> {
                    var i: Int = cs.cellX
                    while (i < cs.cellX + cs.spanX) {
                        if (bottomEdge[i] == cs.cellY) {
                            return true
                        }
                        i++
                    }
                }
            }
            return false
        }

        fun shift(whichEdge: Int, delta: Int) {
            for (v in views) {
                val c: CellAndSpan = config.map.get(v)!!
                when (whichEdge) {
                    LEFT -> c.cellX -= delta
                    RIGHT -> c.cellX += delta
                    TOP -> c.cellY -= delta
                    BOTTOM -> c.cellY += delta
                    else -> c.cellY += delta
                }
            }
            resetEdges()
        }

        fun addView(v: View) {
            views.add(v)
            resetEdges()
        }

        fun getBoundingRectArea(): Rect {
            if (boundingRectDirty) {
                config.getBoundingRectForViews(views, boundingRect)
            }
            return boundingRect
        }

        val comparator: PositionComparator = PositionComparator()

        init {
            resetEdges()
        }

        inner class PositionComparator : Comparator<View> {
            var whichEdge: Int = 0
            override fun compare(left: View, right: View): Int {
                val l: CellAndSpan = config.map.get(left)!!
                val r: CellAndSpan = config.map.get(right)!!
                when (whichEdge) {
                    LEFT -> return (r.cellX + r.spanX) - (l.cellX + l.spanX)
                    RIGHT -> return l.cellX - r.cellX
                    TOP -> return (r.cellY + r.spanY) - (l.cellY + l.spanY)
                    BOTTOM -> return l.cellY - r.cellY
                    else -> return l.cellY - r.cellY
                }
            }
        }

        fun sortConfigurationForEdgePush(edge: Int) {
            comparator.whichEdge = edge
            Collections.sort(config.sortedViews, comparator)
        }


    }

    private fun pushViewsToTempLocation(
        views: ArrayList<View>, rectOccupiedByPotentialDrop: Rect,
        direction: IntArray, dragView: View?, currentState: ItemConfiguration
    ): Boolean {
        val cluster = ViewCluster(views, currentState)
        var clusterRect = cluster.getBoundingRectArea()
        val whichEdge: Int
        var pushDistance: Int
        var fail = false

        // Determine the edge of the cluster that will be leading the push and how far
        // the cluster must be shifted.
        if (direction[0] < 0) {
            whichEdge = LEFT
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left
        } else if (direction[0] > 0) {
            whichEdge = RIGHT
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left
        } else if (direction[1] < 0) {
            whichEdge = TOP
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top
        } else {
            whichEdge = BOTTOM
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top
        }

        // Break early for invalid push distance.
        if (pushDistance <= 0) {
            return false
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (v in views) {
            val c: CellAndSpan = currentState.map.get(v)!!
            mTmpOccupied.markCells(c, false)
        }

        // We save the current configuration -- if we fail to find a solution we will revert
        // to the initial state. The process of finding a solution modifies the configuration
        // in place, hence the need for revert in the failure case.
        currentState.save()

        // The pushing algorithm is simplified by considering the views in the order in which
        // they would be pushed by the cluster. For example, if the cluster is leading with its
        // left edge, we consider sort the views by their right edge, from right to left.
        cluster.sortConfigurationForEdgePush(whichEdge)

        while (pushDistance > 0 && !fail) {
            for (v in currentState.sortedViews) {
                // For each view that isn't in the cluster, we see if the leading edge of the
                // cluster is contacting the edge of that view. If so, we add that view to the
                // cluster.
                if (!cluster.views.contains(v) && v !== dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        val lp = v!!.layoutParams as LayoutParams
                        if (!lp.canReorder) {
                            // The push solution includes the all apps button, this is not viable.
                            fail = true
                            break
                        }
                        cluster.addView(v)
                        val c: CellAndSpan = currentState.map.get(v)!!

                        // Adding view to cluster, mark it as not occupied.
                        mTmpOccupied.markCells(c, false)
                    }
                }
            }
            pushDistance--

            // The cluster has been completed, now we move the whole thing over in the appropriate
            // direction.
            cluster.shift(whichEdge, 1)
        }

        var foundSolution = false
        clusterRect = cluster.getBoundingRectArea()

        // Due to the nature of the algorithm, the only check required to verify a valid solution
        // is to ensure that completed shifted cluster lies completely within the cell layout.
        if (!fail && clusterRect.left >= 0 && clusterRect.right <= this.countX && clusterRect.top >= 0 && clusterRect.bottom <= this.countY) {
            foundSolution = true
        } else {
            currentState.restore()
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (v in cluster.views) {
            val c: CellAndSpan = currentState.map.get(v)!!
            mTmpOccupied.markCells(c, true)
        }

        return foundSolution
    }

    private fun addViewsToTempLocation(
        views: ArrayList<View>, rectOccupiedByPotentialDrop: Rect,
        direction: IntArray, dragView: View?, currentState: ItemConfiguration
    ): Boolean {
        if (views.isEmpty()) return true

        var success = false
        val boundingRect = Rect()
        // We construct a rect which represents the entire group of views passed in
        currentState.getBoundingRectForViews(views, boundingRect)

        // Mark the occupied state as false for the group of views we want to move.
        for (v in views) {
            val c: CellAndSpan = currentState.map.get(v)!!
            mTmpOccupied.markCells(c, false)
        }

        val blockOccupied: GridOccupancy =
            GridOccupancy(boundingRect.width(), boundingRect.height())
        val top = boundingRect.top
        val left = boundingRect.left
        // We mark more precisely which parts of the bounding rect are truly occupied, allowing
        // for interlocking.
        for (v in views) {
            val c: CellAndSpan = currentState.map.get(v)!!
            blockOccupied.markCells(c.cellX - left, c.cellY - top, c.spanX, c.spanY, true)
        }

        mTmpOccupied.markCells(rectOccupiedByPotentialDrop, true)

        findNearestArea(
            boundingRect.left, boundingRect.top, boundingRect.width(),
            boundingRect.height(), direction,
            mTmpOccupied.cells, blockOccupied.cells, mTempLocation
        )

        // If we successfuly found a location by pushing the block of views, we commit it
        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            val deltaX = mTempLocation[0] - boundingRect.left
            val deltaY = mTempLocation[1] - boundingRect.top
            for (v in views) {
                val c: CellAndSpan = currentState.map.get(v)!!
                c.cellX += deltaX
                c.cellY += deltaY
            }
            success = true
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (v in views) {
            val c: CellAndSpan = currentState.map.get(v)!!
            mTmpOccupied.markCells(c, true)
        }
        return success
    }

    // This method tries to find a reordering solution which satisfies the push mechanic by trying
    // to push items in each of the cardinal directions, in an order based on the direction vector
    // passed.
    private fun attemptPushInDirection(
        intersectingViews: ArrayList<View>, occupied: Rect,
        direction: IntArray, ignoreView: View?, solution: ItemConfiguration
    ): Boolean {
        if ((abs(direction[0]) + abs(direction[1])) > 1) {
            // If the direction vector has two non-zero components, we try pushing
            // separately in each of the components.
            var temp = direction[1]
            direction[1] = 0

            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }
            direction[1] = temp
            temp = direction[0]
            direction[0] = 0

            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }
            // Revert the direction
            direction[0] = temp

            // Now we try pushing in each component of the opposite direction
            direction[0] *= -1
            direction[1] *= -1
            temp = direction[1]
            direction[1] = 0
            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }

            direction[1] = temp
            temp = direction[0]
            direction[0] = 0
            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }
            // revert the direction
            direction[0] = temp
            direction[0] *= -1
            direction[1] *= -1
        } else {
            // If the direction vector has a single non-zero component, we push first in the
            // direction of the vector
            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }
            // Then we try the opposite direction
            direction[0] *= -1
            direction[1] *= -1
            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }
            // Switch the direction back
            direction[0] *= -1
            direction[1] *= -1

            // If we have failed to find a push solution with the above, then we try
            // to find a solution by pushing along the perpendicular axis.

            // Swap the components
            var temp = direction[1]
            direction[1] = direction[0]
            direction[0] = temp
            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }

            // Then we try the opposite direction
            direction[0] *= -1
            direction[1] *= -1
            if (pushViewsToTempLocation(
                    intersectingViews, occupied, direction,
                    ignoreView, solution
                )
            ) {
                return true
            }
            // Switch the direction back
            direction[0] *= -1
            direction[1] *= -1

            // Swap the components back
            temp = direction[1]
            direction[1] = direction[0]
            direction[0] = temp
        }
        return false
    }

    private fun rearrangementExists(
        cellX: Int, cellY: Int, spanX: Int, spanY: Int, direction: IntArray,
        ignoreView: View?, solution: ItemConfiguration
    ): Boolean {
        // Return early if get invalid cell positions
        if (cellX < 0 || cellY < 0) return false

        mIntersectingViews.clear()
        mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY)

        // Mark the desired location of the view currently being dragged.
        if (ignoreView != null) {
            val c: CellAndSpan? = solution.map.get(ignoreView)
            if (c != null) {
                c.cellX = cellX
                c.cellY = cellY
            }
        }
        val r0 = Rect(cellX, cellY, cellX + spanX, cellY + spanY)
        val r1 = Rect()
        for (child in solution.map.keys) {
            if (child === ignoreView) continue
            val c: CellAndSpan = solution.map.get(child)!!
            val lp = child.layoutParams as LayoutParams
            r1.set(c.cellX, c.cellY, c.cellX + c.spanX, c.cellY + c.spanY)
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false
                }
                mIntersectingViews.add(child)
            }
        }

        solution.intersectingViews = ArrayList(mIntersectingViews)

        // First we try to find a solution which respects the push mechanic. That is,
        // we try to find a solution such that no displaced item travels through another item
        // without also displacing that item.
        if (attemptPushInDirection(
                mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution
            )
        ) {
            return true
        }

        // Next we try moving the views as a block, but without requiring the push mechanic.
        if (addViewsToTempLocation(
                mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution
            )
        ) {
            return true
        }

        // Ok, they couldn't move as a block, let's move them individually
        for (v in mIntersectingViews) {
            if (!addViewToTempLocation(v, mOccupiedRect, direction, solution)) {
                return false
            }
        }
        return true
    }

    /*
     * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
     * the provided point and the provided cell
     */
    private fun computeDirectionVector(
        deltaX: kotlin.Float,
        deltaY: kotlin.Float,
        result: IntArray
    ) {
        val angle = atan((deltaY / deltaX).toDouble())

        result[0] = 0
        result[1] = 0
        if (abs(cos(angle)) > 0.5f) {
            result[0] = sign(deltaX).toInt()
        }
        if (abs(sin(angle)) > 0.5f) {
            result[1] = sign(deltaY).toInt()
        }
    }

    private fun findReorderSolution(
        pixelX: Int, pixelY: Int, minSpanX: Int, minSpanY: Int,
        spanX: Int, spanY: Int, direction: IntArray, dragView: View?, decX: Boolean,
        solution: ItemConfiguration
    ): ItemConfiguration {
        // Copy the current state into the solution. This solution will be manipulated as necessary.
        copyCurrentStateToSolution(solution, false)
        // Copy the current occupied array into the temporary occupied array. This array will be
        // manipulated as necessary to find a solution.
        mOccupied.copyTo(mTmpOccupied)

        // We find the nearest cell into which we would place the dragged item, assuming there's
        // nothing in its way.
        var result: IntArray? = IntArray(2)
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result)

        val success: Boolean
        // First we try the exact nearest position of the item being dragged,
        // we will then want to try to move this around to other neighbouring positions
        success = rearrangementExists(
            result[0], result[1], spanX, spanY, direction, dragView,
            solution
        )

        if (!success) {
            // We try shrinking the widget down to size in an alternating pattern, shrink 1 in
            // x, then 1 in y etc.
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return findReorderSolution(
                    pixelX, pixelY, minSpanX, minSpanY, spanX - 1, spanY,
                    direction, dragView, false, solution
                )
            } else if (spanY > minSpanY) {
                return findReorderSolution(
                    pixelX, pixelY, minSpanX, minSpanY, spanX, spanY - 1,
                    direction, dragView, true, solution
                )
            }
            solution.isSolution = false
        } else {
            solution.isSolution = true
            solution.cellX = result[0]
            solution.cellY = result[1]
            solution.spanX = spanX
            solution.spanY = spanY
        }
        return solution
    }

    private fun copyCurrentStateToSolution(solution: ItemConfiguration, temp: Boolean) {
        val childCount: Int = mShortcutsAndWidgets.getChildCount()
        for (i in 0..<childCount) {
            val child: View = mShortcutsAndWidgets.getChildAt(i)
            val lp = child.getLayoutParams() as LayoutParams
            val c: CellAndSpan?
            if (temp) {
                c = CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan)
            } else {
                c = CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan)
            }
            solution.add(child, c)
        }
    }

    private fun copySolutionToTempState(solution: ItemConfiguration, dragView: View?) {
        mTmpOccupied.clear()

        val childCount: Int = mShortcutsAndWidgets.size
        for (i in 0..<childCount) {
            val child: View = mShortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val lp = child.getLayoutParams() as LayoutParams
            val c: CellAndSpan? = solution.map.get(child)
            if (c != null) {
                lp.tmpCellX = c.cellX
                lp.tmpCellY = c.cellY
                lp.cellHSpan = c.spanX
                lp.cellVSpan = c.spanY
                mTmpOccupied.markCells(c, true)
            }
        }
        mTmpOccupied.markCells(solution, true)
    }

    private fun animateItemsToSolution(
        solution: ItemConfiguration,
        dragView: View?,
        commitDragView: Boolean
    ) {
        val occupied: GridOccupancy = if (DESTRUCTIVE_REORDER) mOccupied else mTmpOccupied
        occupied.clear()

        val childCount: Int = mShortcutsAndWidgets.size
        for (i in 0..<childCount) {
            val child: View? = mShortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val c: CellAndSpan? = solution.map.get(child)
            if (c != null) {
                animateChildToPosition(
                    child, c.cellX, c.cellY, REORDER_ANIMATION_DURATION, 0,
                    DESTRUCTIVE_REORDER, false
                )
                occupied.markCells(c, true)
            }
        }
        if (commitDragView) {
            occupied.markCells(solution, true)
        }
    }


    // This method starts or changes the reorder preview animations
    private fun beginOrAdjustReorderPreviewAnimations(
        solution: ItemConfiguration,
        dragView: View?, mode: Int
    ) {
        val childCount: Int = mShortcutsAndWidgets.size
        for (i in 0..<childCount) {
            val child: View = mShortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val c: CellAndSpan? = solution.map.get(child)
            val skip =
                mode == MODE_HINT && (solution.intersectingViews
                        != null) && !solution.intersectingViews!!.contains(child)


            val lp = child.layoutParams as LayoutParams
            if (c != null && !skip && (child is Reorderable)) {
                val rha = ReorderPreviewAnimation(
                    child as Reorderable,
                    mode, lp.cellX, lp.cellY, c.cellX, c.cellY, c.spanX, c.spanY
                )
                rha.animate()
            }
        }
    }

    init {


        setWillNotDraw(false)
        clipToPadding = false
        this.cellHeight = -1
        this.cellWidth = this.cellHeight
        this.mBorderSpacing = DisplayUtil.dpToPx(context, DeviceProfile.INV.BORDER_SPACING.toFloat())
        mFixedCellHeight = -1
        mFixedCellWidth = mFixedCellHeight

        this.countX = DeviceProfile.INV.NUM_COLUMNS
        this.countY = DeviceProfile.INV.NUM_ROWS
        mOccupied = GridOccupancy(this.countX, this.countY)
        mTmpOccupied = GridOccupancy(this.countX, this.countY)

        mPreviousReorderDirection[0] = INVALID_DIRECTION
        mPreviousReorderDirection[1] = INVALID_DIRECTION
        val res = resources

        this.scrimBackground = res.getDrawable(R.drawable.bg_celllayout)
        scrimBackground.callback = this
        scrimBackground.alpha = 0

        mReorderPreviewAnimationMagnitude = (REORDER_PREVIEW_MAGNITUDE * DeviceProfile.ICON_SIZE_DP)

        // Initialize the data structures used for the drag visualization.
        mEaseOutInterpolator = Interpolators.DEACCEL_2_5 // Quint ease out
        mDragCell[1] = -1
        mDragCell[0] = mDragCell[1]
        for (i in mDragOutlines.indices) {
            mDragOutlines[i] = Rect(-1, -1, -1, -1)
        }
        mDragOutlinePaint.setColor(ContextCompat.getColor(context, R.color.workspace_text_color_light))

        val duration: Int = res.getInteger(R.integer.config_dragOutlineFadeTime)
        val fromAlphaValue = 0f
        val toAlphaValue = res.getInteger(R.integer.config_dragOutlineMaxAlpha) as kotlin.Float

        Arrays.fill(mDragOutlineAlphas, fromAlphaValue)

        for (i in mDragOutlineAnims.indices) {
            val anim =
                InterruptibleInOutAnimator(duration.toLong(), fromAlphaValue, toAlphaValue)
            anim.animator.interpolator = mEaseOutInterpolator
            val thisIndex = i
            anim.animator.addUpdateListener { animation ->
                val outline: Bitmap? = anim.tag as Bitmap?

                // If an animation is started and then stopped very quickly, we can still
                // get spurious updates we've cleared the tag. Guard against this.
                if (outline == null) {
                    if (LOGD) {
                        val `val`: Any? = animation.getAnimatedValue()
                        Timber.tag(TAG).d(
                            "anim " + thisIndex + " update: " + `val` +
                                    ", isStopped " + anim.isStopped()
                        )
                    }
                    // Try to prevent it from continuing to run
                    animation.cancel()
                } else {
                    mDragOutlineAlphas[thisIndex] =
                        (animation.getAnimatedValue() as kotlin.Float?)!!
                    this@CellLayout.invalidate(mDragOutlines[thisIndex])
                }
            }
            // The animation holds a reference to the drag outline bitmap as long is it's
            // running. This way the bitmap can be GCed when the animations are complete.
            anim.animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if ((animation as ValueAnimator).getAnimatedValue() as Float? == 0f) {
                        anim.tag = null
                    }
                }
            })
            mDragOutlineAnims[i] = anim
        }

        mShortcutsAndWidgets = ShortcutAndWidgetContainer(context)
        mShortcutsAndWidgets.setCellDimensions(
            this.cellWidth,
            this.cellHeight,
            this.countX,
            this.countY
        )
        addView(mShortcutsAndWidgets)
    }

    // Class which represents the reorder preview animations. These animations show that an item is
    // in a temporary state, and hint at where the item will return to.
    inner class ReorderPreviewAnimation(
        child: Reorderable, mode: Int, cellX0: Int, cellY0: Int,
        cellX1: Int, cellY1: Int, spanX: Int, spanY: Int
    ) {
        private val PREVIEW_DURATION = 300
        private val HINT_DURATION: Int = 650

        private val CHILD_DIVIDEND = 4.0f




        val child: Reorderable
        var finalDeltaX: Float
        var finalDeltaY: Float
        var initDeltaX: Float
        var initDeltaY: Float
        val finalScale: Float
        var initScale: Float
        val mode: Int
        var repeating: Boolean = false
        var animationProgress: Float = 0f
            set(value) {
                field = value
                val r1 = if (mode == MODE_HINT && repeating) 1.0f else animationProgress
                val x = r1 * finalDeltaX + (1 - r1) * initDeltaX
                val y = r1 * finalDeltaY + (1 - r1) * initDeltaY
                child.setReorderBounceOffset(x, y)
                val s = animationProgress * finalScale + (1 - animationProgress) * initScale
                child.setReorderBounceScale(s)
            }
        var a: ValueAnimator? = null

        init {
            regionToCenterPoint(cellX0, cellY0, spanX, spanY, mTmpPoint)
            val x0 = mTmpPoint[0]
            val y0 = mTmpPoint[1]
            regionToCenterPoint(cellX1, cellY1, spanX, spanY, mTmpPoint)
            val x1 = mTmpPoint[0]
            val y1 = mTmpPoint[1]
            val dX = x1 - x0
            val dY = y1 - y0

            this.child = child
            this.mode = mode
            finalDeltaX = 0f
            finalDeltaY = 0f

            child.getReorderBounceOffset(mTmpPointF)
            initDeltaX = mTmpPointF.x
            initDeltaY = mTmpPointF.y
            initScale = child.getReorderBounceScale()
            finalScale = mChildScale - (CHILD_DIVIDEND / child.getView().getWidth()) * initScale

            val dir = if (mode == MODE_HINT) -1 else 1
            if (dX == dY && dX == 0) {
            } else {
                if (dY == 0) {
                    finalDeltaX = -dir * sign(dX.toFloat()) * mReorderPreviewAnimationMagnitude
                } else if (dX == 0) {
                    finalDeltaY = -dir * sign(dY.toFloat()) * mReorderPreviewAnimationMagnitude
                } else {
                    val angle = atan(((dY).toFloat() / dX).toDouble())
                    finalDeltaX =
                        ((-dir * sign(dX.toFloat()) * abs(cos(angle) * mReorderPreviewAnimationMagnitude))).toInt()
                            .toFloat()
                    finalDeltaY =
                        ((-dir * sign(dY.toFloat()) * abs(sin(angle) * mReorderPreviewAnimationMagnitude))).toInt()
                            .toFloat()
                }
            }
        }

        fun setInitialAnimationValuesToBaseline() {
            initScale = mChildScale
            initDeltaX = 0f
            initDeltaY = 0f
        }

        fun animate() {
            val noMovement = (finalDeltaX == 0f) && (finalDeltaY == 0f)

            if (mShakeAnimators.containsKey(child)) {
                val oldAnimation = mShakeAnimators.get(child)
                mShakeAnimators.remove(child)

                if (noMovement) {
                    // A previous animation for this item exists, and no new animation will exist.
                    // Finish the old animation smoothly.
                    oldAnimation!!.finishAnimation()
                    return
                } else {
                    // A previous animation for this item exists, and a new one will exist. Stop
                    // the old animation in its tracks, and proceed with the new one.
                    oldAnimation!!.cancel()
                }
            }
            if (noMovement) {
                return
            }

            val va: ValueAnimator =
                ObjectAnimator.ofFloat<ReorderPreviewAnimation?>(this, ANIMATION_PROGRESS, 0f, 1f)
            a = va

            if (areAnimationsEnabled(context)) {
                va.repeatMode = ValueAnimator.REVERSE
                va.repeatCount = ValueAnimator.INFINITE
            }

            va.setDuration((if (mode == MODE_HINT) HINT_DURATION else PREVIEW_DURATION).toLong())
            va.setStartDelay((Math.random() * 60).toInt().toLong())
            va.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: Animator) {
                    // We make sure to end only after a full period
                    setInitialAnimationValuesToBaseline()
                    repeating = true
                }
            })
            mShakeAnimators.put(child, this)
            va.start()
        }


        private fun cancel() {
            a?.cancel()
        }

        /**
         * Smoothly returns the item to its baseline position / scale
         */
        fun finishAnimation() {
            a?.cancel()

            setInitialAnimationValuesToBaseline()
            val va: ValueAnimator = ObjectAnimator.ofFloat<ReorderPreviewAnimation?>(
                this, ANIMATION_PROGRESS,
                animationProgress, 0f
            )
            va.setInterpolator(InterpolatorsAndroidX.DECELERATE_1_5)
            va.setDuration(REORDER_ANIMATION_DURATION.toLong())
            va.start()
        }

    }

    private fun completeAndClearReorderPreviewAnimations() {
        for (a in mShakeAnimators.values) {
            a.finishAnimation()
        }
        mShakeAnimators.clear()
    }

    private fun commitTempPlacement() {
        mTmpOccupied.copyTo(mOccupied)

        val childCount: Int = mShortcutsAndWidgets.size
        for (i in 0..<childCount) {
            val child: View = mShortcutsAndWidgets.getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            val info = child.tag as ItemInfo?
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null) {
                val requiresDbUpdate =
                    (info.cellX != lp.tmpCellX || info.cellY != lp.tmpCellY || info.spanX != lp.cellHSpan || info.spanY != lp.cellVSpan)

                lp.cellX = lp.tmpCellX
                info.cellX = lp.cellX
                lp.cellY = lp.tmpCellY
                info.cellY = lp.cellY
                info.spanX = lp.cellHSpan
                info.spanY = lp.cellVSpan
            }
        }
    }

    private fun setUseTempCoords(useTempCoords: Boolean) {
        val childCount: Int = mShortcutsAndWidgets.size
        for (i in 0..<childCount) {
            val lp = mShortcutsAndWidgets.getChildAt(i).getLayoutParams() as LayoutParams
            lp.useTmpCoords = useTempCoords
        }
    }

    private fun findConfigurationNoShuffle(
        pixelX: Int, pixelY: Int, minSpanX: Int, minSpanY: Int,
        spanX: Int, spanY: Int, dragView: View?, solution: ItemConfiguration
    ): ItemConfiguration {
        val result = IntArray(2)
        val resultSpan = IntArray(2)
        findNearestVacantArea(
            pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, result,
            resultSpan
        )
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false)
            solution.cellX = result[0]
            solution.cellY = result[1]
            solution.spanX = resultSpan[0]
            solution.spanY = resultSpan[1]
            solution.isSolution = true
        } else {
            solution.isSolution = false
        }
        return solution
    }

    private fun getDirectionVectorForDrop(
        dragViewCenterX: Int, dragViewCenterY: Int, spanX: Int,
        spanY: Int, dragView: View?, resultDirection: IntArray
    ) {
        val targetDestination = IntArray(2)

        findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination)
        val dragRect = Rect()
        regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect)
        dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY())

        val dropRegionRect = Rect()
        getViewsIntersectingRegion(
            targetDestination[0], targetDestination[1], spanX, spanY,
            dragView, dropRegionRect, mIntersectingViews
        )

        val dropRegionSpanX = dropRegionRect.width()
        val dropRegionSpanY = dropRegionRect.height()

        regionToRect(
            dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
            dropRegionRect.height(), dropRegionRect
        )

        var deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX
        var deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY

        if (dropRegionSpanX == this.countX || spanX == this.countX) {
            deltaX = 0
        }
        if (dropRegionSpanY == this.countY || spanY == this.countY) {
            deltaY = 0
        }

        if (deltaX == 0 && deltaY == 0) {
            // No idea what to do, give a random direction.
            resultDirection[0] = 1
            resultDirection[1] = 0
        } else {
            computeDirectionVector(deltaX.toFloat(), deltaY.toFloat(), resultDirection)
        }
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    private fun getViewsIntersectingRegion(
        cellX: Int, cellY: Int, spanX: Int, spanY: Int,
        dragView: View?, boundingRect: Rect?, intersectingViews: ArrayList<View>
    ) {
        boundingRect?.set(cellX, cellY, cellX + spanX, cellY + spanY)
        intersectingViews.clear()
        val r0 = Rect(cellX, cellY, cellX + spanX, cellY + spanY)
        val r1 = Rect()
        val count: Int = mShortcutsAndWidgets.size
        for (i in 0..<count) {
            val child: View = mShortcutsAndWidgets.getChildAt(i)
            if (child === dragView) continue
            val lp = child.getLayoutParams() as LayoutParams
            r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan)
            if (Rect.intersects(r0, r1)) {
                mIntersectingViews.add(child)
                boundingRect?.union(r1)
            }
        }
    }

    fun isNearestDropLocationOccupied(
        pixelX: Int, pixelY: Int, spanX: Int, spanY: Int,
        dragView: View?, result: IntArray
    ): Boolean {
        var result = result
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result)
        getViewsIntersectingRegion(
            result[0], result[1], spanX, spanY, dragView, null,
            mIntersectingViews
        )
        return !mIntersectingViews.isEmpty()
    }

    fun revertTempState() {
        completeAndClearReorderPreviewAnimations()
        if (this.isItemPlacementDirty) {
            val count: Int = mShortcutsAndWidgets.size
            for (i in 0..<count) {
                val child: View = mShortcutsAndWidgets.getChildAt(i)
                val lp = child.getLayoutParams() as LayoutParams
                if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                    lp.tmpCellX = lp.cellX
                    lp.tmpCellY = lp.cellY
                    animateChildToPosition(
                        child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION,
                        0, false, false
                    )
                }
            }
            this.isItemPlacementDirty = false
        }
    }

    fun createAreaForResize(
        cellX: Int, cellY: Int, spanX: Int, spanY: Int,
        dragView: View?, direction: IntArray, commit: Boolean
    ): Boolean {
        val pixelXY = IntArray(2)
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY)

        // First we determine if things have moved enough to cause a different layout
        val swapSolution = findReorderSolution(
            pixelXY[0], pixelXY[1], spanX, spanY,
            spanX, spanY, direction, dragView, true, ItemConfiguration()
        )

        setUseTempCoords(true)
        if (swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView)
            this.isItemPlacementDirty = true
            animateItemsToSolution(swapSolution, dragView, commit)

            if (commit) {
                commitTempPlacement()
                completeAndClearReorderPreviewAnimations()
                this.isItemPlacementDirty = false
            } else {
                beginOrAdjustReorderPreviewAnimations(
                    swapSolution, dragView,
                    MODE_PREVIEW
                )
            }
            mShortcutsAndWidgets.requestLayout()
        }
        return swapSolution.isSolution
    }

    fun performReorder(
        pixelX: Int, pixelY: Int, minSpanX: Int, minSpanY: Int, spanX: Int, spanY: Int,
        dragView: View?, result: IntArray, resultSpan: IntArray?, mode: Int
    ): IntArray {
        // First we determine if things have moved enough to cause a different layout
        var result = result
        var resultSpan = resultSpan
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result)

        if (resultSpan == null) {
            resultSpan = IntArray(2)
        }

        // When we are checking drop validity or actually dropping, we don't recompute the
        // direction vector, since we want the solution to match the preview, and it's possible
        // that the exact position of the item has changed to result in a new reordering outcome.
        if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL || mode == MODE_ACCEPT_DROP)
            && mPreviousReorderDirection[0] != INVALID_DIRECTION
        ) {
            mDirectionVector[0] = mPreviousReorderDirection[0]
            mDirectionVector[1] = mPreviousReorderDirection[1]
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousReorderDirection[0] = INVALID_DIRECTION
                mPreviousReorderDirection[1] = INVALID_DIRECTION
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanX, spanY, dragView, mDirectionVector)
            mPreviousReorderDirection[0] = mDirectionVector[0]
            mPreviousReorderDirection[1] = mDirectionVector[1]
        }

        // Find a solution involving pushing / displacing any items in the way
        val swapSolution = findReorderSolution(
            pixelX, pixelY, minSpanX, minSpanY,
            spanX, spanY, mDirectionVector, dragView, true, ItemConfiguration()
        )

        // We attempt the approach which doesn't shuffle views at all
        val noShuffleSolution = findConfigurationNoShuffle(
            pixelX, pixelY, minSpanX,
            minSpanY, spanX, spanY, dragView, ItemConfiguration()
        )

        var finalSolution: ItemConfiguration? = null

        // If the reorder solution requires resizing (shrinking) the item being dropped, we instead
        // favor a solution in which the item is not resized, but
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution
        }

        if (mode == MODE_SHOW_REORDER_HINT) {
            if (finalSolution != null) {
                beginOrAdjustReorderPreviewAnimations(
                    finalSolution, dragView,
                    MODE_HINT
                )
                result[0] = finalSolution.cellX
                result[1] = finalSolution.cellY
                resultSpan[0] = finalSolution.spanX
                resultSpan[1] = finalSolution.spanY
            } else {
                resultSpan[1] = -1
                resultSpan[0] = resultSpan[1]
                result[1] = resultSpan[0]
                result[0] = result[1]
            }
            return result
        }

        var foundSolution = true
        setUseTempCoords(true)

        if (finalSolution != null) {
            result[0] = finalSolution.cellX
            result[1] = finalSolution.cellY
            resultSpan[0] = finalSolution.spanX
            resultSpan[1] = finalSolution.spanY

            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                copySolutionToTempState(finalSolution, dragView)
                this.isItemPlacementDirty = true
                animateItemsToSolution(finalSolution, dragView, mode == MODE_ON_DROP)

                if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL
                ) {
                    commitTempPlacement()
                    completeAndClearReorderPreviewAnimations()
                    this.isItemPlacementDirty = false
                } else {
                    beginOrAdjustReorderPreviewAnimations(
                        finalSolution, dragView,
                        MODE_PREVIEW
                    )
                }
            }
        } else {
            foundSolution = false
            resultSpan[1] = -1
            resultSpan[0] = resultSpan[1]
            result[1] = resultSpan[0]
            result[0] = result[1]
        }

        if (mode == MODE_ON_DROP || !foundSolution) {
            setUseTempCoords(false)
        }

        mShortcutsAndWidgets.requestLayout()
        return result
    }

    private class ItemConfiguration : CellAndSpan() {
        val map: ArrayMap<View, CellAndSpan?> = ArrayMap<View, CellAndSpan?>()
        private val savedMap: ArrayMap<View?, CellAndSpan?> = ArrayMap<View?, CellAndSpan?>()
        val sortedViews: ArrayList<View?> = ArrayList()
        var intersectingViews: ArrayList<View?>? = null
        var isSolution: Boolean = false

        fun save() {
            // Copy current state into savedMap
            for (v in map.keys) {
                savedMap.get(v)?.copyFrom(map.get(v)!!)
            }
        }

        fun restore() {
            // Restore current state from savedMap
            for (v in savedMap.keys) {
                map.get(v)?.copyFrom(savedMap.get(v)!!)
            }
        }

        fun add(v: View, cs: CellAndSpan) {
            map.put(v, cs)
            savedMap.put(v, CellAndSpan())
            sortedViews.add(v)
        }

        fun area(): Int {
            return spanX * spanY
        }

        fun getBoundingRectForViews(views: ArrayList<View>, outRect: Rect) {
            var first = true
            for (v in views) {
                val c: CellAndSpan = map.get(v)!!
                if (first) {
                    outRect.set(c.cellX, c.cellY, c.cellX + c.spanX, c.cellY + c.spanY)
                    first = false
                } else {
                    outRect.union(c.cellX, c.cellY, c.cellX + c.spanX, c.cellY + c.spanY)
                }
            }
        }
    }

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    fun findNearestArea(
        pixelX: Int,
        pixelY: Int,
        spanX: Int,
        spanY: Int,
        result: IntArray?
    ): IntArray {
        return findNearestArea(pixelX, pixelY, spanX, spanY, spanX, spanY, false, result, null)
    }

    fun existsEmptyCell(): Boolean {
        return findCellForSpan(null, 1, 1)
    }

    /**
     * Finds the upper-left coordinate of the first rectangle in the grid that can
     * hold a cell of the specified dimensions. If intersectX and intersectY are not -1,
     * then this method will only return coordinates for rectangles that contain the cell
     * (intersectX, intersectY)
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     * can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    fun findCellForSpan(cellXY: IntArray?, spanX: Int, spanY: Int): Boolean {
        var cellXY = cellXY
        if (cellXY == null) {
            cellXY = IntArray(2)
        }
        return mOccupied.findVacantCell(cellXY, spanX, spanY)
    }

    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    fun onDragEnter() {
        mDragging = true
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    fun onDragExit() {
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false
        }

        // Invalidate the drag data
        mDragCell[1] = -1
        mDragCell[0] = mDragCell[1]
        mDragOutlineAnims[mDragOutlineCurrent]?.animateOut()
        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.size
        revertTempState()
        this.isDragOverlapping = false
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    fun onDropChild(child: View?) {
        if (child != null) {
            val lp = child.getLayoutParams() as LayoutParams
            lp.dropped = true
            child.requestLayout()
            markCellsAsOccupiedForView(child)
        }
    }

    /**
     * Computes a bounding rectangle for a range of cells
     *
     * @param cellX X coordinate of upper left corner expressed as a cell position
     * @param cellY Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan Width in cells
     * @param cellVSpan Height in cells
     * @param resultRect Rect into which to put the results
     */
    fun cellToRect(cellX: Int, cellY: Int, cellHSpan: Int, cellVSpan: Int, resultRect: Rect) {
        val cellWidth = this.cellWidth
        val cellHeight = this.cellHeight

        val hStartPadding = getPaddingLeft()
        val vStartPadding = getPaddingTop()

        val width = cellHSpan * cellWidth
        val height = cellVSpan * cellHeight
        val x = hStartPadding + cellX * cellWidth
        val y = vStartPadding + cellY * cellHeight

        resultRect.set(x, y, x + width, y + height)
    }

    fun markCellsAsOccupiedForView(view: View?) {
        if (view == null || view.parent !== mShortcutsAndWidgets) return
        val lp = view.layoutParams as LayoutParams
        mOccupied.markCells(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, true)
    }

    fun markCellsAsUnoccupiedForView(view: View?) {
        if (view == null || view.getParent() !== mShortcutsAndWidgets) return
        val lp = view.getLayoutParams() as LayoutParams
        mOccupied.markCells(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, false)
    }

    val desiredWidth: Int
        get() = getPaddingLeft() + getPaddingRight() + (this.countX * this.cellWidth)

    val desiredHeight: Int
        get() = getPaddingTop() + getPaddingBottom() + (this.countY * this.cellHeight)

    fun isOccupied(x: Int, y: Int): Boolean {
        if (x < this.countX && y < this.countY) {
            return mOccupied.cells[x][y]
        } else {
            throw RuntimeException("Position exceeds the bound of this CellLayout")
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(getContext(), attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    class LayoutParams : MarginLayoutParams {

        var cellX: Int = 0


        var cellY: Int = 0

        /**
         * Temporary horizontal location of the item in the grid during reorder
         */
        var tmpCellX: Int = 0

        /**
         * Temporary vertical location of the item in the grid during reorder
         */
        var tmpCellY: Int = 0

        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        var useTmpCoords: Boolean = false

        /**
         * Number of cells spanned horizontally by the item.
         */

        var cellHSpan: Int


        var cellVSpan: Int

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        var isLockedToGrid: Boolean = true

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button and QSB place holder.
         */
        var canReorder: Boolean = true

        var x: Int = 0

        var y: Int = 0

        var dropped: Boolean = false

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            cellHSpan = 1
            cellVSpan = 1
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source) {
            cellHSpan = 1
            cellVSpan = 1
        }

        constructor(source: LayoutParams) : super(source) {
            this.cellX = source.cellX
            this.cellY = source.cellY
            this.cellHSpan = source.cellHSpan
            this.cellVSpan = source.cellVSpan
        }

        constructor(cellX: Int, cellY: Int, cellHSpan: Int, cellVSpan: Int) : super(
            MATCH_PARENT,
            MATCH_PARENT
        ) {
            this.cellX = cellX
            this.cellY = cellY
            this.cellHSpan = cellHSpan
            this.cellVSpan = cellVSpan
        }

        /**
         * Use this method, as opposed to [.setup], if the view needs
         * to be scaled.
         *
         * ie. In multi-window mode, we setup widgets so that they are measured and laid out
         * using their full/invariant device profile sizes.
         */
        @JvmOverloads
        fun setup(
            cellWidth: Int, cellHeight: Int,
            cellScaleX: Float = 1.0f, cellScaleY: Float = 1.0f
        ) {
            if (isLockedToGrid) {
                val myCellHSpan = cellHSpan
                val myCellVSpan = cellVSpan
                var myCellX = if (useTmpCoords) tmpCellX else cellX
                val myCellY = if (useTmpCoords) tmpCellY else cellY

                width = (myCellHSpan * cellWidth / cellScaleX - leftMargin - rightMargin).toInt()
                height = (myCellVSpan * cellHeight / cellScaleY - topMargin - bottomMargin).toInt()
                x = (myCellX * cellWidth + leftMargin)
                y = (myCellY * cellHeight + topMargin)
            }
        }

        /**
         * Sets the position to the provided point
         */
        fun setXY(point: Point) {
            cellX = point.x
            cellY = point.y
        }

        override fun toString(): String {
            return "(" + this.cellX + ", " + this.cellY + ")"
        }
    }

    class CellInfo(v: View?, info: ItemInfo) : CellAndSpan() {
        val cell: View?
        init {
            cellX = info.cellX
            cellY = info.cellY
            spanX = info.spanX
            spanY = info.spanY
            cell = v
        }

        public override fun toString(): String {
            return ("Cell[view=" + (cell?.javaClass ?: "null")
                    + ", x=" + cellX + ", y=" + cellY + "]")
        }
    }

    /**
     * A Delegated cell Drawing for drawing on CellLayout
     */
    abstract class DelegatedCellDrawing {
        var mDelegateCellX: Int = 0
        var mDelegateCellY: Int = 0

        /**
         * Draw under CellLayout
         */
        abstract fun drawUnderItem(canvas: Canvas?)

        /**
         * Draw over CellLayout
         */
        abstract fun drawOverItem(canvas: Canvas?)
    }

    /**
     * Returns whether an item can be placed in this CellLayout (after rearranging and/or resizing
     * if necessary).
     */
    fun hasReorderSolution(itemInfo: ItemInfo): Boolean {
        val cellPoint = IntArray(2)
        // Check for a solution starting at every cell.
        for (cellX in 0..<this.countX) {
            for (cellY in 0..<this.countY) {
                cellToPoint(cellX, cellY, cellPoint)
                if (findReorderSolution(
                        cellPoint[0], cellPoint[1], itemInfo.minSpanX,
                        itemInfo.minSpanY, itemInfo.spanX, itemInfo.spanY, mDirectionVector, null,
                        true, ItemConfiguration()
                    ).isSolution
                ) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Finds solution to accept hotseat migration to cell layout. commits solution if commitConfig
     */
    fun makeSpaceForHotseatMigration(commitConfig: Boolean): Boolean {
        val cellPoint = IntArray(2)
        val directionVector = intArrayOf(0, -1)
        cellToPoint(0, this.countY, cellPoint)
        val configuration = ItemConfiguration()
        if (findReorderSolution(
                cellPoint[0], cellPoint[1], this.countX, 1, this.countX, 1,
                directionVector, null, false, configuration
            ).isSolution
        ) {
            if (commitConfig) {
                copySolutionToTempState(configuration, null)
                commitTempPlacement()
                // undo marking cells occupied since there is actually nothing being placed yet.
                mOccupied.markCells(0, this.countY - 1, this.countX, 1, false)
            }
            return true
        }
        return false
    }

    /**
     * returns a copy of cell layout's grid occupancy
     */
    fun cloneGridOccupancy(): GridOccupancy {
        val occupancy: GridOccupancy = GridOccupancy(this.countX, this.countY)
        mOccupied.copyTo(occupancy)
        return occupancy
    }

    fun isRegionVacant(x: Int, y: Int, spanX: Int, spanY: Int): Boolean {
        return mOccupied.isRegionVacant(x, y, spanX, spanY)
    }

    companion object {
        private const val TAG = "CellLayout"
        private const val LOGD = false

        // Used to visualize / debug the Grid of the CellLayout
        private const val VISUALIZE_GRID = false
        private val BACKGROUND_STATE_ACTIVE = intArrayOf(android.R.attr.state_active)
        private val BACKGROUND_STATE_DEFAULT: IntArray? = EMPTY_STATE_SET
        const val WORKSPACE: Int = 0
        const val HOTSEAT: Int = 1
        const val FOLDER: Int = 2

        const val MODE_SHOW_REORDER_HINT: Int = 0
        const val MODE_DRAG_OVER: Int = 1
        const val MODE_ON_DROP: Int = 2
        const val MODE_ON_DROP_EXTERNAL: Int = 3
        const val MODE_ACCEPT_DROP: Int = 4
        private const val DESTRUCTIVE_REORDER = false
        private const val DEBUG_VISUALIZE_OCCUPIED = false

        private const val REORDER_PREVIEW_MAGNITUDE = 0.12f
        private const val REORDER_ANIMATION_DURATION = 150
        private val INVALID_DIRECTION = -100

        private val sPaint = Paint()

        const val MODE_HINT: Int = 0
        const val MODE_PREVIEW: Int = 1

        private val ANIMATION_PROGRESS: Property<ReorderPreviewAnimation, Float> = object :
            Property<ReorderPreviewAnimation, Float>(
                Float::class.javaPrimitiveType,
                "animationProgress"
            ) {
            override fun get(anim: ReorderPreviewAnimation): Float {
                return anim.animationProgress
            }

            override fun set(anim: ReorderPreviewAnimation, progress: Float) {
                anim.animationProgress = progress
            }
        }
    }
}

object ViewClusterConfig {
    const val LEFT: Int = 1 shl 0
    const val TOP: Int = 1 shl 1
    const val RIGHT: Int = 1 shl 2
    const val BOTTOM: Int = 1 shl 3
}

