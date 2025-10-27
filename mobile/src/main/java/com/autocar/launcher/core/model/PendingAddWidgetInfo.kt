package com.autocar.launcher.core.model

import com.autocar.launcher.core.model.data.UserThemePlugin
import com.autocar.launcher.core.widget.WidgetCardView

/**
 * 准备添加到桌面的Widget
 */
class PendingAddWidgetInfo(
    var userThemePlugin: UserThemePlugin?= null,
    var widgetCardView: WidgetCardView?= null
): PendingAddItemInfo()