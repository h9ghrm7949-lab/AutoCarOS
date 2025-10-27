package com.autocar.launcher.core.icons

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Nullable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * DeferredDrawable:
 * 异步加载 Drawable（基于 Glide），
 * 自动淡入显示，可包装成 FastBitmapDrawable。
 */
class DeferredDrawable(
    private val context: Context,
    private val url: String?,
    private val placeholder: Drawable? = null,
    private val fadeDuration: Long = 300L
) : Drawable() {

    private var currentDrawable: Drawable? = placeholder
    private var alphaProgress: Float = if (placeholder == null) 0f else 1f
    private var fadeAnimator: ValueAnimator? = null

    init {
        setUrl(url)
    }

    private fun setUrl(url: String?) {
        if (url.isNullOrEmpty()) return

        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, @Nullable transition: Transition<in Bitmap>?) {
                    startFadeIn(FastBitmapDrawable(resource))
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    currentDrawable = placeholder
                    invalidateSelf()
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    /** 启动淡入动画 */
    private fun startFadeIn(resource: Drawable) {
        currentDrawable = resource.apply { bounds = this@DeferredDrawable.bounds }

        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = fadeDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                alphaProgress = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    override fun draw(canvas: Canvas) {
        val drawable = currentDrawable ?: return
        drawable.bounds = bounds

        val saveCount = canvas.saveLayer(null, null)
        drawable.alpha = (alphaProgress * 255).toInt()
        drawable.draw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun setAlpha(alpha: Int) {
        currentDrawable?.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        currentDrawable?.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return currentDrawable?.opacity ?: PixelFormat.TRANSPARENT
    }
}