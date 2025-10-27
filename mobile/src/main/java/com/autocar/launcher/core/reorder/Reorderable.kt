package com.autocar.launcher.core.reorder

import android.graphics.PointF
import android.view.View


interface Reorderable {

    fun setReorderBounceOffset(x: Float, y: Float)

    fun getReorderBounceOffset(offset: PointF)

    fun setReorderPreviewOffset(x: Float, y: Float)

    fun getReorderPreviewOffset(offset: PointF)

    fun setReorderBounceScale(scale: Float)

    fun getReorderBounceScale(): Float

    fun getView(): View

}