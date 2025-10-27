package com.autocar.launcher.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver.OnDrawListener
import com.autocar.launcher.core.util.Utils.SINGLE_FRAME_MS
import timber.log.Timber
import java.lang.Float
import kotlin.Boolean
import kotlin.Long

class FirstFrameAnimatorHelper : AnimatorListenerAdapter, AnimatorUpdateListener {
    private val mTarget: View
    private var mStartFrame: Long = 0
    private var mStartTime: Long = -1
    private var mHandlingOnAnimationUpdate = false
    private var mAdjustedSecondFrameTime = false

    constructor(animator: ValueAnimator, target: View) {
        mTarget = target
        animator.addUpdateListener(this)
    }

    constructor(vpa: ViewPropertyAnimator, target: View) {
        mTarget = target
        vpa.setListener(this)
    }

    // only used for ViewPropertyAnimators
    override fun onAnimationStart(animation: Animator) {
        val va = animation as ValueAnimator
        va.addUpdateListener(this@FirstFrameAnimatorHelper)
        onAnimationUpdate(va)
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val currentTime = System.currentTimeMillis()
        if (mStartTime == -1L) {
            mStartFrame = sGlobalFrameCounter
            mStartTime = currentTime
        }

        val currentPlayTime = animation.getCurrentPlayTime()
        val isFinalFrame = Float.compare(1f, animation.getAnimatedFraction()) == 0

        if (!mHandlingOnAnimationUpdate &&
            sVisible && currentPlayTime < animation.getDuration() && !isFinalFrame
        ) {
            mHandlingOnAnimationUpdate = true
            val frameNum: Long = sGlobalFrameCounter - mStartFrame
            if (frameNum == 0L && currentTime < mStartTime + MAX_DELAY && currentPlayTime > 0) {
                mTarget.getRootView().invalidate()
                animation.setCurrentPlayTime(0)
            } else if (frameNum == 1L && currentTime < mStartTime + MAX_DELAY && !mAdjustedSecondFrameTime && currentTime > mStartTime + SINGLE_FRAME_MS && currentPlayTime > SINGLE_FRAME_MS) {
                animation.setCurrentPlayTime(SINGLE_FRAME_MS.toLong())
                mAdjustedSecondFrameTime = true
            } else {
                if (frameNum > 1) {
                    mTarget.post { animation.removeUpdateListener(this@FirstFrameAnimatorHelper) }
                }
                if (DEBUG) print(animation)
            }
            mHandlingOnAnimationUpdate = false
        } else {
            if (DEBUG) print(animation)
        }
    }

    fun print(animation: ValueAnimator) {
        val flatFraction = animation.getCurrentPlayTime() / animation.getDuration().toFloat()
        Timber.tag(TAG).d(
            "%s%s", sGlobalFrameCounter.toString() +
                    "(" + (sGlobalFrameCounter - mStartFrame) + ") " + mTarget + " dirty? " +
                    mTarget.isDirty() + " " + flatFraction + " " + this + " ", animation
        )
    }

    companion object {
        private const val TAG = "FirstFrameAnimatorHlpr"
        private const val DEBUG = false
        private const val MAX_DELAY = 1000
        private var sGlobalDrawListener: OnDrawListener? = null
        var sGlobalFrameCounter: Long = 0
        private var sVisible = false

        @JvmStatic
        fun setIsVisible(visible: Boolean) {
            sVisible = visible
        }

        @JvmStatic
        fun initializeDrawListener(view: View) {
            if (sGlobalDrawListener != null) {
                view.getViewTreeObserver().removeOnDrawListener(sGlobalDrawListener)
            }

            sGlobalDrawListener = OnDrawListener { sGlobalFrameCounter++ }
            view.getViewTreeObserver().addOnDrawListener(sGlobalDrawListener)
            sVisible = true
        }
    }
}
