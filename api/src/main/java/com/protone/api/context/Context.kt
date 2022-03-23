package com.protone.api.context

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.*

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

val Context.root: ViewGroup?
    get() {
        return when (this) {
            is Activity -> {
                findViewById(android.R.id.content)
            }
            else -> null
        }
    }

val todayTime: String
    get() {
        return SimpleDateFormat(
            "yyyy/MM/dd",
            Locale.getDefault()
        ).format(Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = System.currentTimeMillis()
        }.time)
    }


fun onBackground(function: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        Thread {
            function()
        }.start()
    } else function()
}

fun Int.to16to9Width(): Int {
    return this * 16 / 9
}

fun Int.to16to9Height(): Int {
    return this * 9 / 16
}

fun Long.toDate(): Date? {
    return longToDate(this, "yyyy-MM-dd HH:mm:ss")
}

fun dateToString(data: Date?, formatType: String?): String? {
    return data?.run { SimpleDateFormat(formatType, Locale.CHINA).format(this) }
}

fun longToString(currentTime: Long, formatType: String): String? {
    return dateToString(longToDate(currentTime, formatType), formatType)
}

fun stringToDate(strTime: String?, formatType: String?): Date? {
    val formatter = SimpleDateFormat(formatType, Locale.CHINA)
    var date: Date? = null
    strTime?.let {
        date = formatter.parse(it)
    }
    return date
}

fun longToDate(currentTime: Long, formatType: String): Date? {
    return stringToDate(dateToString(Date(currentTime), formatType), formatType)
}
