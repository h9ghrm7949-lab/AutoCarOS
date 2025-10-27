package com.autocar.launcher.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.autocar.launcher.R
import com.autocar.launcher.core.base.BaseActivity
import com.autocar.launcher.ui.LauncherActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity: BaseActivity() {
    override fun attachLayoutRes(): Int = R.layout.activity_splash

    override fun initData(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            delay(5000)
            startActivity(Intent(this@SplashActivity, LauncherActivity::class.java))
        }
    }

    override fun initView() {

    }
}