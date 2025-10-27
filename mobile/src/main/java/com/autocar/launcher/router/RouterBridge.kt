package com.autocar.launcher.router

/**
 * 提供常用的安全调用封装
 * - call：无返回值的安全调用
 * - callResult： 有返回值的安全调用
 */
object RouterBridge {
    inline fun <reified T> call(path: String, crossinline block: (T) -> Unit) {
        Router.call<T>(path) {
            block(it)
        }
    }

    inline fun <reified T,R> callResult(path: String, crossinline block: (T) -> R): R? {
        var result: R ?= null
        Router.call<T>(path) {
            result = block(it)
        }
        return result
    }
}