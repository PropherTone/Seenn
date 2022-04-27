package com.protone.api

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import com.protone.api.context.Global
import com.protone.api.context.onBackground
import com.protone.api.context.onUiThread
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors

fun String.getFileName(): String {
    return this.split("/").run {
        this[this.size - 1]
    }
}

fun String.toDrawable(context: Context, callBack: (Drawable?) -> Unit) {
    onBackground {
        val file = File(this)
        val drawable: Drawable? = if (!file.isFile || !file.exists()) null
        else BitmapDrawable.createFromPath(this)
        context.onUiThread { callBack.invoke(drawable) }
    }
}

fun String.getParentPath(): String {
    return this.substring(0, lastIndexOf("/"))
}

fun <T> List<T>.toSplitString(split: String): String = this.stream().map(java.lang.String::valueOf)
    .collect(Collectors.joining(split))


fun Uri.toMediaBitmapByteArray(): ByteArray? {
    var byteArray: ByteArray? = null
    var ois = Global.application.contentResolver.openInputStream(this) ?: return byteArray
    val os = ByteArrayOutputStream()
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(ois, null, options)
        ois.close()
        ois = Global.application.contentResolver.openInputStream(this) ?: return byteArray
        byteArray = options.let {
            val dimensionPixelSize =
                Global.application.resources.getDimensionPixelSize(R.dimen.huge_icon)
            it.inSampleSize = calculateInSampleSize(it, dimensionPixelSize, dimensionPixelSize)
            it.inJustDecodeBounds = false
            BitmapFactory
                .decodeStream(ois, null, it)
                ?.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.toByteArray()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        ois.close()
        os.close()
    }
    return byteArray
}

private fun calculateInSampleSize(option: BitmapFactory.Options, h: Int, w: Int): Int {
    val outWidth = option.outWidth
    val outHeight = option.outHeight
    var sampleSize = 1
    if (outHeight > h || outWidth > w) {
        val halfHeight = outHeight / 2
        val halfWidth = outWidth / 2
        while ((halfHeight / sampleSize) >= h && (halfWidth / sampleSize) >= w) {
            sampleSize *= 2
        }
    }
    return sampleSize
}

fun Uri.toBitmapByteArray(): ByteArray? {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    var embeddedPicture: ByteArray? = null
    try {
        mediaMetadataRetriever.setDataSource(Global.application, this)
        embeddedPicture = mediaMetadataRetriever.embeddedPicture
        embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
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
    return "${musicTime / 60}:${if (sec >= 10) sec else "0$sec"}"
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

@ChecksSdkIntAtLeast(api = 32)
fun upSDK31() = Build.VERSION.SDK_INT > 31