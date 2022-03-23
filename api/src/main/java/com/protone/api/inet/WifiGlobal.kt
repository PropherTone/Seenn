package com.protone.api.inet

import android.content.Context
import android.net.wifi.WifiManager
import com.protone.api.context.Global

fun Context.isWifiEnable(): Boolean {
    return (Global.application.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
}
