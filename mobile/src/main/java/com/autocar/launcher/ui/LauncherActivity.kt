package com.autocar.launcher.ui

import android.app.Activity
import android.os.Bundle
import android.os.PersistableBundle
import com.autocar.launcher.R

class LauncherActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_launcher)
    }

}