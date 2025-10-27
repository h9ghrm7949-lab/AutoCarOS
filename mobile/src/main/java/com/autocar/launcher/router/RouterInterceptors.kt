package com.autocar.launcher.router


/**
 * 拦截器管理器
 * 支持按照priority 插入（priority大的先执行）
 */
object RouterInterceptors {

    private data class Entry(val priority: Int, val interceptor: RouterInterceptor)

    private val list = mutableListOf<Entry>()

    @Synchronized
    fun add(interceptor: RouterInterceptor, priority: Int = 0) {
        val idx = list.indexOfFirst {
            it.priority < priority
        }
        if (idx == -1) {
            list.add(Entry(priority, interceptor))
        } else {
            list.add(idx, Entry(priority, interceptor))
        }
    }

    @Synchronized
    fun remove(interceptor: RouterInterceptor) {
        list.removeAll { it.interceptor === interceptor }
    }

    @Synchronized
    fun clear() {
        list.clear();
    }

    fun proceed(path: String, finalAction: () -> Unit) {
        val snapshot = synchronized(this) {
            list.toList()
        }
        var index = 0;
        fun next() {
            if (index < snapshot.size) {
                val current = snapshot[index++]
                try {
                    val intercepted = current.interceptor.intercept(path) {
                        next()
                    }
                    // 如果拦截器返回true，表示已经处理完毕，不继续
                    if (intercepted) {
                        // TODO 待定
                    }
                } catch (t: Throwable) {
                    // 忽略异常下一个
                    next()
                }
            }
         }
        next()
    }

}