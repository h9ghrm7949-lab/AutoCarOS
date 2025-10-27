package com.autocar.launcher.router

/**
 * 路由拦截器
 */
fun interface RouterInterceptor {
    /**
     * @param path 路由路径
     * @param proceed 若继续执行则需要点用proceed()
     * @return 返回true则表示本拦截器已经“完全处理”该请求（中断后继续执行）
     * 返回 false表示未中断（由拦截器内部选择是否调用proceed）
     */
    fun intercept(path: String, proceed: () -> Unit): Boolean
}