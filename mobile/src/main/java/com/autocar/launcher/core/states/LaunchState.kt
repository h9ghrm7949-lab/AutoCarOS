package com.autocar.launcher.core.states

/**
 * 定义车载系统下的三种Launcher状态
 * - 驾驶模式
 * - 停车模式
 * - 编辑模式
 */
sealed class LaunchState(val name: String = "") {

    /**
     * 停车模式
     */
    data object ParkingMode: LaunchState("parkingMode")

    /**
     * 驾驶模式
     */
    data object DrivingMode: LaunchState("drivingMode")

    /**
     * 编辑模式
     */
    data object EditMode: LaunchState("editMode")

}