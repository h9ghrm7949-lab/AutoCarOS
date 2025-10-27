package com.autocar.launcher.router

/**
 * 路径常量集中处理
 */
object RouterPath {
    object Service {
        // AI服务
        const val AI = "car://service/ai"
        // 主题服务
        const val THEME = "car://service/theme"
        // 通知服务
        const val NOTIFICATION = "car://service/notification"
        // 壁纸服务
        const val WALLPAPER = "car://service/wallpaper"
        // 账号服务
        const val ACCOUNT = "car://service/account"
    }
    object UI {
        // 主页
        const val HOME = "car://ui/home"
        // 设置页面
        const val SETTING = "car://ui/setting"
        // 启动页
        const val SPLASH = "car://ui/splash"
        // 应用商店
        const val APP_STORE = "car://ui/appstore"
        // 主题商店
        const val THEME_STORE = "car://ui/theme_store"
    }
}