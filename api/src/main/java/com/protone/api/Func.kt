package com.protone.api

import android.content.pm.ApplicationInfo
import com.protone.api.context.SApplication
import java.text.SimpleDateFormat
import java.util.*

fun isInDebug(): Boolean {
    return try {
        val info: ApplicationInfo = SApplication.app.applicationInfo
        (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun todayDate(format: String): String = SimpleDateFormat(
    format,
    Locale.getDefault()
).format(Calendar.getInstance(Locale.getDefault()).apply {
    timeInMillis = System.currentTimeMillis()
}.time)

val sizeFormatMap = mapOf(Pair(0, "B"), Pair(1, "KB"), Pair(2, "MB"), Pair(3, "GB"))