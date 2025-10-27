package com.autocar.launcher.core.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Property;


public class InterruptibleInOutAnimator {

    private static final Property<InterruptibleInOutAnimator, Float> VALUE =
            new Property<InterruptibleInOutAnimator, Float>(Float.TYPE, "value") {
                @Override
                public Float get(InterruptibleInOutAnimator anim) {
                    return anim.mValue;
                }

                @Override
                public void set(InterruptibleInOutAnimator anim, Float value) {
                    anim.mValue = value;
                }
            };

    private long mOriginalDuration;
    private float mOriginalFromValue;
    private float mOriginalToValue;
    private ValueAnimator mAnimator;

    private float mValue;

    private boolean mFirstRun = true;

    private Object mTag = null;

    private static final int STOPPED = 0;
    private static final int IN = 1;
    private static final int OUT = 2;

    int mDirection = STOPPED;

    public InterruptibleInOutAnimator(long duration, float fromValue, float toValue) {
        mAnimator = ObjectAnimator.ofFloat(this, VALUE, fromValue, toValue).setDuration(duration);
        mOriginalDuration = duration;
        mOriginalFromValue = fromValue;
        mOriginalToValue = toValue;

        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDirection = STOPPED;
            }
        });
    }

    private void animate(int direction) {
        final long currentPlayTime = mAnimator.getCurrentPlayTime();
        final float toValue = (direction == IN) ? mOriginalToValue : mOriginalFromValue;
        final float startValue = mFirstRun ? mOriginalFromValue : mValue;

        // Make sure it's stopped before we modify any values
        cancel();

        // TODO: We don't really need to do the animation if startValue == toValue, but
        // somehow that doesn't seem to work, possibly a quirk of the animation framework
        mDirection = direction;

        // Ensure we don't calculate a non-sensical duration
        long duration = mOriginalDuration - currentPlayTime;
        mAnimator.setDuration(Math.max(0, Math.min(duration, mOriginalDuration)));

        mAnimator.setFloatValues(startValue, toValue);
        mAnimator.start();
        mFirstRun = false;
    }

    public void cancel() {
        mAnimator.cancel();
        mDirection = STOPPED;
    }

    public void end() {
        mAnimator.end();
        mDirection = STOPPED;
    }

    /**
     * Return true when the animation is not running and it hasn't even been started.
     */
    public boolean isStopped() {
        return mDirection == STOPPED;
    }

    /**
     * This is the equivalent of calling Animator.start(), except that it can be called when
     * the animation is running in the opposite direction, in which case we reverse
     * direction and animate for a correspondingly shorter duration.
     */
    public void animateIn() {
        animate(IN);
    }

    /**
     * This is the roughly the equivalent of calling Animator.reverse(), except that it uses the
     * same interpolation curve as animateIn(), rather than mirroring it. Also, like animateIn(),
     * if the animation is currently running in the opposite direction, we reverse
     * direction and animate for a correspondingly shorter duration.
     */
    public void animateOut() {
        animate(OUT);
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public Object getTag() {
        return mTag;
    }

    public ValueAnimator getAnimator() {
        return mAnimator;
    }
}
