package com.autocar.launcher

import android.app.Application
import com.king.logx.LogX
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
        initLog()
    }

    private fun initLog() {
        Timber.plant(object: Timber.DebugTree() {
            // 初始化日志打印
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                LogX.offset(4).log(priority, message)
            }

        })
    }

    companion object {
        var context: Application ?= null
    }



}