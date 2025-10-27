package com.autocar.launcher.core.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.autocar.launcher.core.widget.WorkspaceItemView
import com.autocar.launcher.core.model.data.AppItemInfo
import com.autocar.launcher.core.model.data.ItemInfo
import com.autocar.launcher.core.util.GridConfig

class AppCardView(
    context: Context,
    attrs: AttributeSet?= null,
    defStyleAttr: Int = 0
): WorkspaceItemView(context,attrs,defStyleAttr) {

    /**
     * 绑定应用信息和网格配置
     */
    fun bind(info: AppItemInfo, gridConfig: GridConfig) {
        tag = info
        // 创建图标
        val iconView = ImageView(context).apply {
            setImageDrawable(info.appShortcut?.drawable)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        // 设置布局参数
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            val margin = gridConfig.cellPadding
            setMargins(margin, margin, margin, margin)
        }
        // 添加到布局中
        addView(iconView, lp)
        // 设置点击事件：启动应用
        setOnClickListener {
            val intent = info.makeLaunchIntent(info.componentName)
            context.startActivity(intent)
        }
    }

    override fun getViewType(): Int = ItemInfo.ItemType.APP
}