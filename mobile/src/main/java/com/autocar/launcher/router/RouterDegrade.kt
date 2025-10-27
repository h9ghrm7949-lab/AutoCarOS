package com.autocar.launcher.router

/**
 * 路由降级借口
 * 当某路径无法实现的时候，Router会调用这里进行降级处理（可返回代理实现或null）
 */
fun interface RouterDegrade {
    fun onMissing(path: String): Any?
}