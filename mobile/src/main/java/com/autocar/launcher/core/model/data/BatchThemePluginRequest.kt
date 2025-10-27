package com.autocar.launcher.core.model.data

import com.autocar.launcher.model.data.BatchThemePluginInfo
import com.google.gson.annotations.SerializedName

class BatchThemePluginRequest(
    @SerializedName("theme_id")
    var themeId: Int = 0,
    @SerializedName("plugins")
    var plugins: List<BatchThemePluginInfo> = mutableListOf<BatchThemePluginInfo>()
) {
    override fun toString(): String {
        return "BatchThemePluginRequest(themeId=$themeId, plugins=$plugins)"
    }
}