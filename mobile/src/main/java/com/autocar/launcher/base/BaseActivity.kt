package com.autocar.launcher.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(attachLayoutRes())
        initView()
        initData(savedInstanceState)
    }

    /**
     * 布局文件id
     */
    protected abstract fun attachLayoutRes(): Int

    /**
     * 初始化数据
     */
    abstract fun initData(savedInstanceState: Bundle?)
    /**
     * 初始化 View
     */
    abstract fun initView()


}