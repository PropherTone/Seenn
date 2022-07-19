package com.protone.api.inet

import android.content.Context
import android.net.wifi.WifiManager
import com.protone.api.context.SApplication

fun Context.isWifiEnable(): Boolean {
    return (SApplication.app.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
}
