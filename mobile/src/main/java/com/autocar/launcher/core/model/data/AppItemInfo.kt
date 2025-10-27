package com.autocar.launcher.core.model.data

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo


/**
 * 桌面组件 -应用程序单图标
 */
class AppItemInfo(
    var appShortcut: AppShortcut?= null,
): ItemInfo(itemType = ItemType.APP) {
    override fun toString(): String {
        return "AppItemInfo(appShortcut=$appShortcut) ${super.toString()}"
    }

    fun makeLaunchIntent(info: LauncherActivityInfo): Intent {
        return makeLaunchIntent(info.componentName)
    }

    fun makeLaunchIntent(cn: ComponentName?): Intent {
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(cn)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

}