package com.autocar.launcher.router

/**
 * Router核心：注册/注销/查找/调用
 */
object Router {
    val registry = mutableMapOf<String, Any>()

    @Volatile
    var degradeHandler: RouterDegrade ?= null

    /**
     * 注册实例,覆盖旧路径
     */
    @Synchronized
    fun register(path: String, instance: Any) {
        registry[path] = instance
    }

    @Synchronized
    fun unRegister(path: String) {
        registry.remove(path)
    }

    /**
     * 是否包含路径
     */
    @Synchronized
    fun contains(path: String): Boolean = registry.containsKey(path)

    inline fun <reified T> route(path: String): T? {
        val found = synchronized(this) {
            registry[path]
        }
        if (found != null && found is T) return found
        // 降级
        val deg = degradeHandler?.onMissing(path)
        return deg as? T
    }

    inline fun <reified T> call(path: String, crossinline block: (T) -> Unit) {
        RouterInterceptors.proceed(path) {
            val instance = route<T>(path)
        }
    }

    /**
     * 获取原始Any实例
     */
    @Synchronized
    fun raw(path: String): Any? = registry[path]

}