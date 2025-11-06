package com.android.systemui.airconditioningbar.view.windlevel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.autocar.launcher.R

class WindModeArea @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): ConstraintLayout(context, attrs) {
    init {
        addView(LayoutInflater.from(context).inflate(R.layout.ac_bar_wind_mode_area, this, false))
    }
}