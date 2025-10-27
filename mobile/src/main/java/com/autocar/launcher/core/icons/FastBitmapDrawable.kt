/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.autocar.launcher.core.icons


import android.R
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Property
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.graphics.withSave

open class FastBitmapDrawable protected constructor(
    b: Bitmap,
    iconColor: Int,
    isDisabled: Boolean = false
) : Drawable() {
    protected val mPaint: Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    protected var mBitmap: Bitmap? = null

    /**
     * Returns the primary icon color
     */
    val iconColor: Int

    private var mColorFilter: ColorFilter? = null

    private var mIsPressed = false
    protected var mIsDisabled: Boolean = false
    var mDisabledAlpha: Float = 1f

    private var mScaleAnimation: ObjectAnimator? = null
    private var mScale = 1f

    private var mAlpha = 255

    protected var isDisabled: Boolean
        get() = mIsDisabled
        set(isDisabled) {
            if (mIsDisabled != isDisabled) {
                mIsDisabled = isDisabled
                updateFilter()
            }
        }

    constructor(b: Bitmap) : this(b, Color.TRANSPARENT)


    init {
        mBitmap = b
        this.iconColor = iconColor
        setFilterBitmap(true)
        this.isDisabled = isDisabled
    }

    override fun draw(canvas: Canvas) {
        if (mScale != 1f) {
            canvas.withSave {
                val bounds = getBounds()
                canvas.scale(mScale, mScale, bounds.exactCenterX(), bounds.exactCenterY())
                drawInternal(canvas, bounds)
            }
        } else {
            drawInternal(canvas, getBounds())
        }
    }

    protected fun drawInternal(canvas: Canvas, bounds: Rect) {
        canvas.drawBitmap(mBitmap!!, null, bounds, mPaint)
    }

    val isThemed: Boolean
        /**
         * Returns if this represents a themed icon
         */
        get() = false

    override fun setColorFilter(cf: ColorFilter?) {
        mColorFilter = cf
        updateFilter()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {
        if (mAlpha != alpha) {
            mAlpha = alpha
            mPaint.setAlpha(alpha)
            invalidateSelf()
        }
    }

    override fun setFilterBitmap(filterBitmap: Boolean) {
        mPaint.setFilterBitmap(filterBitmap)
        mPaint.setAntiAlias(filterBitmap)
    }

    override fun getAlpha(): Int {
        return mAlpha
    }

    fun resetScale() {
        if (mScaleAnimation != null) {
            mScaleAnimation!!.cancel()
            mScaleAnimation = null
        }
        mScale = 1f
        invalidateSelf()
    }

    val animatedScale: Float
        get() = if (mScaleAnimation == null) 1f else mScale

    override fun getIntrinsicWidth(): Int {
        return mBitmap!!.getWidth()
    }

    override fun getIntrinsicHeight(): Int {
        return mBitmap!!.getHeight()
    }

    override fun getMinimumWidth(): Int {
        return getBounds().width()
    }

    override fun getMinimumHeight(): Int {
        return getBounds().height()
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun getColorFilter(): ColorFilter? {
        return mPaint.colorFilter
    }

    override fun onStateChange(state: IntArray): Boolean {
        var isPressed = false
        for (s in state) {
            if (s == R.attr.state_pressed) {
                isPressed = true
                break
            }
        }
        if (mIsPressed != isPressed) {
            mIsPressed = isPressed

            if (mScaleAnimation != null) {
                mScaleAnimation!!.cancel()
                mScaleAnimation = null
            }

            if (mIsPressed) {
                // Animate when going to pressed state
                mScaleAnimation =
                    ObjectAnimator.ofFloat<FastBitmapDrawable?>(this, SCALE, PRESSED_SCALE)
                mScaleAnimation!!.setDuration(CLICK_FEEDBACK_DURATION.toLong())
                mScaleAnimation!!.setInterpolator(ACCEL)
                mScaleAnimation!!.start()
            } else {
                if (isVisible()) {
                    mScaleAnimation = ObjectAnimator.ofFloat<FastBitmapDrawable?>(this, SCALE, 1f)
                    mScaleAnimation!!.setDuration(CLICK_FEEDBACK_DURATION.toLong())
                    mScaleAnimation!!.setInterpolator(DEACCEL)
                    mScaleAnimation!!.start()
                } else {
                    mScale = 1f
                    invalidateSelf()
                }
            }
            return true
        }
        return false
    }


    private val disabledColorFilter: ColorFilter
        get() {
            if (sDisabledFColorFilter == null) {
                sDisabledFColorFilter =
                    getDisabledFColorFilter(
                        mDisabledAlpha
                    )
            }
            return sDisabledFColorFilter!!
        }

    /**
     * Updates the paint to reflect the current brightness and saturation.
     */
    protected fun updateFilter() {
        mPaint.setColorFilter(if (mIsDisabled) this.disabledColorFilter else mColorFilter)
        invalidateSelf()
    }

    override fun getConstantState(): ConstantState? {
        return FastBitmapConstantState(mBitmap!!, this.iconColor, mIsDisabled)
    }

    protected class FastBitmapConstantState(
        protected val mBitmap: Bitmap,
        protected val mIconColor: Int,
        protected val mIsDisabled: Boolean
    ) : ConstantState() {
        override fun newDrawable(): FastBitmapDrawable {
            return FastBitmapDrawable(mBitmap, mIconColor, mIsDisabled)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }
    }

    companion object {
        private val ACCEL: Interpolator = AccelerateInterpolator()
        private val DEACCEL: Interpolator = DecelerateInterpolator()

        private const val PRESSED_SCALE = 1.1f

        private const val DISABLED_DESATURATION = 1f
        private const val DISABLED_BRIGHTNESS = 0.5f

        const val CLICK_FEEDBACK_DURATION: Int = 200

        private var sDisabledFColorFilter: ColorFilter? = null

        // Animator and properties for the fast bitmap drawable's scale
        private val SCALE
                : Property<FastBitmapDrawable, Float> =
            object : Property<FastBitmapDrawable, Float>(java.lang.Float.TYPE, "scale") {
                override fun get(fastBitmapDrawable: FastBitmapDrawable): Float {
                    return fastBitmapDrawable.mScale
                }

                override fun set(fastBitmapDrawable: FastBitmapDrawable, value: Float) {
                    fastBitmapDrawable.mScale = value
                    fastBitmapDrawable.invalidateSelf()
                }
            }

        fun getDisabledFColorFilter(disabledAlpha: Float): ColorFilter {
            val tempBrightnessMatrix = ColorMatrix()
            val tempFilterMatrix = ColorMatrix()

            tempFilterMatrix.setSaturation(1f - DISABLED_DESATURATION)
            val scale: Float = 1 - DISABLED_BRIGHTNESS
            val brightnessI = (255 * DISABLED_BRIGHTNESS).toInt()
            val mat = tempBrightnessMatrix.getArray()
            mat[0] = scale
            mat[6] = scale
            mat[12] = scale
            mat[4] = brightnessI.toFloat()
            mat[9] = brightnessI.toFloat()
            mat[14] = brightnessI.toFloat()
            mat[18] = disabledAlpha
            tempFilterMatrix.preConcat(tempBrightnessMatrix)
            return ColorMatrixColorFilter(tempBrightnessMatrix)
        }
    }
}
