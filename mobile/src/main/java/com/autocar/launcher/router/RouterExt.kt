package com.autocar.launcher.router

inline fun <reified T> route(path: String): T? = Router.route(path)

inline fun <reified T> routeCall(path: String, crossinline block: (T) -> Unit) {
    RouterBridge.call<T>(path) {
        block(it)
    }
}