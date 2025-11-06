package com.android.systemui.status

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.autocar.launcher.R

class StatusBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): ConstraintLayout(context, attrs) {
    init {
        addView(LayoutInflater.from(context).inflate(R.layout.sys_statusbar, this, false))
    }
}