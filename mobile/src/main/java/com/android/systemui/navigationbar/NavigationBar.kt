package com.android.systemui.navigationbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.autocar.launcher.R

class NavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): ConstraintLayout(context, attrs) {
    init {
        addView(LayoutInflater.from(context).inflate(R.layout.sys_navigation_layout, this, false))
    }
}