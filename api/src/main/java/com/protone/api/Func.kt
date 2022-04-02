package com.protone.api

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.protone.api.context.Global
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

fun String.getFileName(): String {
    return this.split("/").run {
        this[this.size - 1]
    }
}

fun String.getParentPath(): String {
    return this.substring(0, lastIndexOf("/"))
}

fun Uri.toBitmapByteArray(): ByteArray? {
    Log.d(TAG, "toBitmapByteArray: $this")
    val mediaMetadataRetriever = MediaMetadataRetriever()
    var embeddedPicture:ByteArray? = null
    try {
        mediaMetadataRetriever.setDataSource(Global.application, this)
        embeddedPicture = mediaMetadataRetriever.embeddedPicture
    }catch (e:Exception){
        e.printStackTrace()
    }finally {
        mediaMetadataRetriever.release()
    }
    return embeddedPicture
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

fun Long.toStringMinuteTime(): String {
    val musicTime: Long = this / 1000
    val sec = musicTime % 60
    return "${musicTime / 60}:${if (sec > 10) sec else "0$sec"}"
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

val todayTime: String
    get() {
        return SimpleDateFormat(
            "yyyy/MM/dd",
            Locale.getDefault()
        ).format(Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = System.currentTimeMillis()
        }.time)
    }