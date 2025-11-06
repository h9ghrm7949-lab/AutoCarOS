package com.android.systemui.voice

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.autocar.launcher.R

class VoiceBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): ConstraintLayout(context, attrs) {
    init {
        addView(LayoutInflater.from(context).inflate(R.layout.voice_bar_pick_area, this, false))
    }
}