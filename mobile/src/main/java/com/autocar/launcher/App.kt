package com.autocar.launcher

import android.app.Application
import com.king.logx.LogX
import dagger.hilt.android.HiltAndroidApp
import me.jessyan.autosize.AutoSizeConfig
import timber.log.Timber

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
        initLog()
        initAutoSize()
    }

    private fun initLog() {
        Timber.plant(object: Timber.DebugTree() {
            // 初始化日志打印
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                LogX.offset(4).log(priority, message)
            }

        })
    }

    private fun initAutoSize() {
        AutoSizeConfig.getInstance()
            .setBaseOnWidth(false)
            .setUseDeviceSize(true)
            .setDesignWidthInDp(1280)
            .setDesignHeightInDp(720)
    }

    companion object {
        var context: Application ?= null
    }



}