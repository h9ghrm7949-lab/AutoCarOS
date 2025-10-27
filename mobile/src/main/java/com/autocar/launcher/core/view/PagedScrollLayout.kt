package com.autocar.launcher.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.max

/**
 * 自定义基础分页滚动容器（抽象类）
 * 主要负责单个子视图的：
 * - 平滑滚动（OverScroller）
 * - 惯性滑动（fling）
 * - 边界回弹
 * - 横向或纵向滑动方向控制
 *
 * @note 后续可扩展成 WorkspacePagedLayout 或 WidgetPagedLayout
 */
abstract class BasePagedScrollLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    /** 是否为横向滚动模式（true 横向 / false 纵向） */
    private val isHorizontalScroll: Boolean = true

    /** 滚动动画控制器 */
    private val scroller: OverScroller = OverScroller(context)

    /** 速度追踪器，用于计算 fling 动作 */
    private var velocityTracker: VelocityTracker? = null

    /** 滑动判定的最小距离（系统配置） */
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    /** fling 的最小 / 最大速度限制 */
    private val minFlingVelocity: Int = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity: Int = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    /** 记录手指上次触摸位置（X 或 Y） */
    private var lastTouchPos: Float = 0f

    /** 是否正在拖拽 */
    private var isBeingDragged = false

    /** 当前活跃指针 ID */
    private var activePointerId = -1

    // ===============================
    // 触摸事件拦截逻辑
    // ===============================
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked

        // 若已在拖拽中，则直接拦截
        if (action == MotionEvent.ACTION_MOVE && isBeingDragged) return true

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录初始触摸点
                lastTouchPos = if (isHorizontalScroll) ev.x else ev.y
                activePointerId = ev.getPointerId(0)
                isBeingDragged = !scroller.isFinished // 若滚动未完成，强制拦截

                // 初始化 VelocityTracker
                velocityTracker = velocityTracker?.apply { clear() } ?: VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return false
                val pos = if (isHorizontalScroll) ev.getX(pointerIndex) else ev.getY(pointerIndex)
                val diff = abs(pos - lastTouchPos)

                if (diff > touchSlop) {
                    isBeingDragged = true
                    lastTouchPos = pos
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                activePointerId = -1
                recycleVelocityTracker()
            }
        }

        return isBeingDragged
    }

    // ===============================
    // 触摸事件处理逻辑
    // ===============================
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        velocityTracker = velocityTracker ?: VelocityTracker.obtain()
        velocityTracker?.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 若当前还在滚动，则立即停止
                if (!scroller.isFinished) scroller.abortAnimation()
                lastTouchPos = if (isHorizontalScroll) ev.x else ev.y
                activePointerId = ev.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return false

                val pos = if (isHorizontalScroll) ev.getX(pointerIndex) else ev.getY(pointerIndex)
                var delta = (lastTouchPos - pos).toInt()

                if (!isBeingDragged && abs(delta) > touchSlop) {
                    isBeingDragged = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    delta = if (delta > 0) delta - touchSlop else delta + touchSlop
                }

                if (isBeingDragged) {
                    lastTouchPos = pos
                    val scrollRange = getMaxScroll()
                    val newScroll = (if (isHorizontalScroll) scrollX else scrollY) + delta
                    val clamped = newScroll.coerceIn(0, scrollRange)

                    if (isHorizontalScroll) scrollTo(clamped, 0)
                    else scrollTo(0, clamped)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isBeingDragged) {
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val velocity = if (isHorizontalScroll)
                        velocityTracker?.getXVelocity(activePointerId)?.toInt() ?: 0
                    else
                        velocityTracker?.getYVelocity(activePointerId)?.toInt() ?: 0

                    if (abs(velocity) > minFlingVelocity) {
                        fling(-velocity)
                    } else {
                        springBack()
                    }
                }
                endTouch()
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) springBack()
                endTouch()
            }
        }
        return true
    }

    // ===============================
    // 滚动与惯性动画
    // ===============================

    /** fling 惯性滑动 */
    private fun fling(velocity: Int) {
        if (isHorizontalScroll) {
            scroller.fling(
                scrollX, 0,
                velocity, 0,
                0, getMaxScrollX(),
                0, 0
            )
        } else {
            scroller.fling(
                0, scrollY,
                0, velocity,
                0, 0,
                0, getMaxScrollY()
            )
        }
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /** 回弹至边界 */
    private fun springBack() {
        if (scroller.springBack(scrollX, scrollY, 0, getMaxScrollX(), 0, getMaxScrollY())) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /** 滚动计算，每帧调用 */
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    // ===============================
    // 测量与布局
    // ===============================
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val child = getChildAt(0)

        child?.let {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(
                max(0, width - paddingLeft - paddingRight),
                if (isHorizontalScroll) MeasureSpec.UNSPECIFIED else MeasureSpec.EXACTLY
            )
            val childHeightSpec = MeasureSpec.makeMeasureSpec(
                max(0, height - paddingTop - paddingBottom),
                if (isHorizontalScroll) MeasureSpec.EXACTLY else MeasureSpec.UNSPECIFIED
            )
            it.measure(childWidthSpec, childHeightSpec)
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val child = getChildAt(0)

        child.layout(
            paddingLeft, paddingTop,
            paddingLeft + child.measuredWidth,
            paddingTop + child.measuredHeight
        )
    }

    // ===============================
    // 辅助函数
    // ===============================
    private fun getMaxScrollX(): Int {
        val child = getChildAt(0)
        return max(0, (child?.measuredWidth ?: 0) - width)
    }

    private fun getMaxScrollY(): Int {
        val child = getChildAt(0)
        return max(0, (child?.measuredHeight ?: 0) - height)
    }

    private fun getMaxScroll(): Int =
        if (isHorizontalScroll) getMaxScrollX() else getMaxScrollY()

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun endTouch() {
        isBeingDragged = false
        activePointerId = -1
        recycleVelocityTracker()
    }
}