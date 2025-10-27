package com.autocar.launcher.core.widget

import android.content.Context
import android.util.AttributeSet
import com.autocar.launcher.core.widget.WorkspaceItemView
import com.autocar.launcher.core.model.data.ItemInfo

class WidgetCardView(
    context: Context,
    attrs: AttributeSet?= null,
    defStyleAttr: Int = 0
): WorkspaceItemView(context,attrs,defStyleAttr) {

    override fun getViewType(): Int = ItemInfo.ItemType.WIDGET
}