package com.autocar.launcher.core.model.data

import com.autocar.launcher.core.icons.DeferredDrawable

data class AppShortcut(
    var packageName: String, // app包名
    var uri: String = "", // uri路径
    var drawable: DeferredDrawable ?= null // icon
)