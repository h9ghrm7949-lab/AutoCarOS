package com.android.systemui.airconditioningbar.view.auto

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.autocar.launcher.R

class TempPickArea @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): ConstraintLayout(context, attrs) {
    init {
        addView(LayoutInflater.from(context).inflate(R.layout.ac_bar_temp_pick_area, this, false))
    }
}