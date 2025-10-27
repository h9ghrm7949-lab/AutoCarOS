package com.autocar.launcher.core.util

import android.graphics.Matrix
import android.os.Build
import android.view.animation.Interpolator
import timber.log.Timber
import kotlin.math.abs

object Utils {

    const val TAG = "utils"

    private val sLoc0: IntArray = IntArray(2)
    private val sLoc1: IntArray = IntArray(2)
    private val sPoint: FloatArray = FloatArray(2)
    private val sMatrix: Matrix = Matrix()
    private val sInverseMatrix: Matrix = Matrix()

    val ATLEAST_P: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    val ATLEAST_OREO_MR1: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    const val ATLEAST_OREO: Boolean = true

    const val ATLEAST_NOUGAT_MR1: Boolean = true

    const val ATLEAST_NOUGAT: Boolean = true

    const val ATLEAST_MARSHMALLOW: Boolean = true
    const val ATLEAST_LOLLIPOP_MR1: Boolean = true

    const val SINGLE_FRAME_MS: Int = 16

    /**
     * Maps t from one range to another range.
     *
     * @param t       The value to map.
     * @param fromMin The lower bound of the range that t is being mapped from.
     * @param fromMax The upper bound of the range that t is being mapped from.
     * @param toMin   The lower bound of the range that t is being mapped to.
     * @param toMax   The upper bound of the range that t is being mapped to.
     * @return The mapped value of t.
     */
    fun mapToRange(
        t: Float, fromMin: Float, fromMax: Float, toMin: Float, toMax: Float,
        interpolator: Interpolator
    ): Float {
        if (fromMin == fromMax || toMin == toMax) {
            Timber.tag(TAG).e("mapToRange: range has 0 length")
            return toMin
        }
        val progress: Float = getProgress(t, fromMin, fromMax)
        return mapRange(interpolator.getInterpolation(progress), toMin, toMax)
    }

    fun mapRange(value: Float, min: Float, max: Float): Float {
        return min + (value * (max - min))
    }

    fun getProgress(current: Float, min: Float, max: Float): Float {
        return abs(current - min) / abs(max - min)
    }
}