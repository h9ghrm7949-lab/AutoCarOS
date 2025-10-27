package com.autocar.launcher.core.widget

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import com.autocar.launcher.core.dragdrop.DraggableView
import com.autocar.launcher.core.model.data.ItemInfo
import com.autocar.launcher.core.reorder.Reorderable

abstract class WorkspaceItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?= null,
    defStyleAttr: Int = 0
): FrameLayout(context,attrs, defStyleAttr), DraggableView, Reorderable {

    private var mScaleToFit = 1f

    private val mTranslationForCentering = PointF(0f, 0f)

    private val mTranslationForReorderBounce = PointF(0f, 0f)
    private val mTranslationForReorderPreview = PointF(0f, 0f)
    private var mScaleForReorderBounce = 1f

    private var mChildrenFocused = false

    override fun getDescendantFocusability(): Int {
        return if (mChildrenFocused)
            FOCUS_BEFORE_DESCENDANTS
        else
            FOCUS_BLOCK_DESCENDANTS
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mChildrenFocused && event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.getAction() == KeyEvent.ACTION_UP) {
            mChildrenFocused = false
            requestFocus()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!mChildrenFocused && keyCode == KeyEvent.KEYCODE_ENTER) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (gainFocus) {
            mChildrenFocused = false
            dispatchChildFocus(false)
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        dispatchChildFocus(mChildrenFocused && focused != null)
        focused?.setFocusableInTouchMode(false)
    }

    override fun clearChildFocus(child: View?) {
        super.clearChildFocus(child)
        dispatchChildFocus(false)
    }

    override fun dispatchUnhandledMove(focused: View?, direction: Int): Boolean {
        return mChildrenFocused
    }

    private fun dispatchChildFocus(childIsFocused: Boolean) {
        isSelected = childIsFocused
    }

    override fun getView(): View {
        return this
    }


    private fun updateTranslation() {
        super.setTranslationX(
            (mTranslationForReorderBounce.x + mTranslationForReorderPreview.x
                    + mTranslationForCentering.x)
        )
        super.setTranslationY(
            (mTranslationForReorderBounce.y + mTranslationForReorderPreview.y
                    + mTranslationForCentering.y)
        )
    }

    fun setTranslationForCentering(x: Float, y: Float) {
        mTranslationForCentering.set(x, y)
        updateTranslation()
    }

    override fun setReorderBounceOffset(x: Float, y: Float) {
        mTranslationForReorderBounce.set(x, y)
        updateTranslation()
    }

    override fun getReorderBounceOffset(offset: PointF) {
        offset.set(mTranslationForReorderBounce)
    }

    override fun setReorderPreviewOffset(x: Float, y: Float) {
        mTranslationForReorderPreview.set(x, y)
        updateTranslation()
    }

    override fun getReorderPreviewOffset(offset: PointF) {
        offset.set(mTranslationForReorderPreview)
    }

    private fun updateScale() {
        super.setScaleX(mScaleToFit * mScaleForReorderBounce)
        super.setScaleY(mScaleToFit * mScaleForReorderBounce)
    }

    override fun setReorderBounceScale(scale: Float) {
        mScaleForReorderBounce = scale
        updateScale()
    }

    override fun getReorderBounceScale(): Float {
        return mScaleForReorderBounce
    }

    fun setScaleToFit(scale: Float) {
        mScaleToFit = scale
        updateScale()
    }

    fun getScaleToFit(): Float {
        return mScaleToFit
    }

    override fun getViewType(): Int {
        return ItemInfo.ItemType.WIDGET
    }

    override fun getWorkspaceVisualDragBounds(bounds: Rect) {
        val width = (measuredWidth * mScaleToFit).toInt()
        val height = (measuredHeight * mScaleToFit).toInt()

        bounds.set(0, 0, width, height)
    }


}