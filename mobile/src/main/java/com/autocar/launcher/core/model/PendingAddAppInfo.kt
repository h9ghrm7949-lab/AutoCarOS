package com.autocar.launcher.core.model

import com.autocar.launcher.core.model.PendingAddItemInfo
import com.autocar.launcher.core.model.data.AppShortcut
import com.autocar.launcher.core.widget.AppCardView

/**
 * 准备添加到桌面的APP - 临时态
 */
class PendingAddAppInfo(
    var appShortcut: AppShortcut?= null,
    var appCardView: AppCardView?= null
): PendingAddItemInfo()