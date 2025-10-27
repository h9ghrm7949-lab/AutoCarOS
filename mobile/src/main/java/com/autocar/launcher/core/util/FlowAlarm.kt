package com.autocar.launcher.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Alarm 一个通过Kotlin + Flow实现的延迟触发器
 * 替代传统的Handler + Listener方案
 * - 支持协程取消
 * - 支持多处监听
 * - 可被生命周期组件 直接collect
 */
class FlowAlarm(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) {

    // 用于广播触发事件的Flow
    private val _alarmFlow = MutableSharedFlow<Unit>(replay = 0)

    val isWaiting: Boolean get() = alarmJob?.isActive == true

    // 当前延迟任务
    private var alarmJob: Job ?= null

    /**
     * 启动一个延时任务
     */
    fun setAlarm(delayMillis: Long) {
        cancelAlarm()
        alarmJob = scope.launch {
            delay(delayMillis)
            _alarmFlow.emit(Unit)
        }
    }

    /**
     * 立即触发
     */
    fun fireImmediately() {
        cancelAlarm()
        scope.launch {
            _alarmFlow.emit(Unit)
        }
    }

    /**
     * 取消任务
     */
    fun cancelAlarm() {
        alarmJob?.cancel()
        alarmJob = null
    }

}