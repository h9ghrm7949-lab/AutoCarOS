package com.autocar.launcher.model.data

import com.google.gson.annotations.SerializedName

class BatchThemePluginInfo(
    @SerializedName("location_x")
    var locationX: Int = -1,
    @SerializedName("location_y")
    var locationY: Int = -1,
    @SerializedName("theme_plugin_id")
    var themePluginId: Int = -1,
    @SerializedName("size_h")
    var sizeH: Int = 1,
    @SerializedName("size_w")
    var sizeW: Int = 1,
    @SerializedName("package_name")
    var packageName: String = ""
) {
    override fun toString(): String {
        return "BatchThemePluginInfo(locationX=$locationX, locationY=$locationY, themePluginId=$themePluginId, sizeH=$sizeH, sizeW=$sizeW, packageName='$packageName')"
    }
}