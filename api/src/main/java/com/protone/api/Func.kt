package com.protone.api

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import com.protone.api.context.APP
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

fun String.getFileName(): String {
    return this.split("/").run { this[this.size - 1] }
}

fun String.getFileMimeType(): String {
    return "." + this.split(".").let { it[it.size - 1] }
}

fun String.getParentPath(): String {
    return this.substring(0, lastIndexOf("/"))
}

fun String.toast() {
    Toast.makeText(APP.app, this, Toast.LENGTH_SHORT).show()
}

fun Bitmap.saveToFile(fileName: String, dir: String? = null): String? {
    var fileOutputStream: FileOutputStream? = null
    return try {
        val tempPath =
            if (dir != null) {
                val dirFile = File("${APP.app.filesDir.absolutePath}/$dir/")
                if (!dirFile.exists() && !dirFile.mkdirs()) {
                    "图片${fileName}.png保存失败!".toast()
                    return null
                }
                "${APP.app.filesDir.absolutePath}/$dir/${fileName.getFileName()}.png"
            } else "${APP.app.filesDir.absolutePath}/${fileName.getFileName()}.png"
        val file = File(tempPath)
        when {
            file.exists() -> tempPath
            file.createNewFile() -> {
                fileOutputStream = FileOutputStream(file)
                this@saveToFile.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                tempPath
            }
            else -> {
                "图片${fileName}.png保存失败!!!".toast()
                null
            }
        }
    } catch (e: Exception) {
        "图片${fileName}.png保存失败".toast()
        null
    } finally {
        try {
            fileOutputStream?.close()
        } catch (e: IOException) {
            if (isInDebug()) e.printStackTrace()
        }
    }
}

fun Uri.saveToFile(
    fileName: String,
    dir: String,
    w: Int = APP.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = APP.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
) = toBitmap(w, h)?.saveToFile(fileName, dir)

fun Uri.toBitmap(
    w: Int = APP.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = APP.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
): Bitmap? {
    var ois: InputStream? = null
    return try {
        ois = APP.app.contentResolver.openInputStream(this) ?: return null
        val bitmap = BitmapFactory.decodeStream(ois, null, null)
        if (bitmap != null) {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                getMatrix(bitmap.width, bitmap.height, w),
                true
            )
        } else null
    } catch (e: Exception) {
        null
    } finally {
        ois?.close()
    }
}

fun Uri.toMediaBitmapByteArray(): ByteArray? {
    var byteArray: ByteArray? = null
    val ois = APP.app.contentResolver.openInputStream(this) ?: return byteArray
    val os = ByteArrayOutputStream()
    try {
        var options = BitmapFactory.Options()
        val dimensionPixelSize =
            APP.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
        options.inJustDecodeBounds = true
        val decodeStream = BitmapFactory.decodeStream(ois, null, options)
        if (decodeStream != null) {
            options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            options.inSampleSize =
                calculateInSampleSize(decodeStream, dimensionPixelSize, dimensionPixelSize)
            BitmapFactory
                .decodeStream(ois, null, options)
            byteArray = os.toByteArray()
        }else return null
    } catch (e: Exception) {
        if (isInDebug()) e.printStackTrace()
    } finally {
        ois.close()
        os.close()
    }
    return byteArray
}

fun Uri.toBitmapByteArray(): ByteArray? {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    var embeddedPicture: ByteArray? = null
    try {
        mediaMetadataRetriever.setDataSource(APP.app, this)
        embeddedPicture = mediaMetadataRetriever.embeddedPicture
    } catch (e: Exception) {
        if (isInDebug()) e.printStackTrace()
    } finally {
        mediaMetadataRetriever.release()
    }
    return embeddedPicture
}

private fun getMatrix(h: Int, w: Int, output: Int): Matrix {
    val rotate = h > w
    val matrix = Matrix()
    var scale = 1f
    val scale1: Float
    val target1: Int
    val revers: Boolean
    val target = if (h > w) {
        target1 = h
        revers = !rotate
        w
    } else {
        target1 = w
        revers = rotate
        h
    }
    if (target > output) {
        scale = target.toFloat() / output
    }
    val fl = target1.toFloat() * scale
    scale1 = fl / output
    if (revers) {
        matrix.setScale(scale, scale1)
    } else {
        matrix.setScale(scale1, scale)
    }
    return matrix
}

private fun calculateInSampleSize(bitmap: Bitmap, h: Int, w: Int): Int {
    val outWidth = bitmap.width
    val outHeight = bitmap.height
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

fun isInDebug(): Boolean {
    return try {
        val info: ApplicationInfo = APP.app.applicationInfo
        (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun Long.toDateString(format: String = "HH:mm:ss yyyy/MM/dd E"): String? =
    SimpleDateFormat(format, Locale.getDefault()).format(
        Calendar.getInstance(Locale.getDefault()).also {
            it.timeInMillis = this * 1000
        }.time
    )

fun Long.toDateString(): String? =
    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(
        Calendar.getInstance(Locale.getDefault()).also {
            it.timeInMillis = this
        }.time
    )

fun Long.toStringMinuteTime(): String {
    val musicTime: Long = this / 1000
    val sec = musicTime % 60
    return "${musicTime / 60}:${if (sec >= 10) sec else "0$sec"}"
}

fun todayDate(format: String): String = SimpleDateFormat(
    format,
    Locale.getDefault()
).format(Calendar.getInstance(Locale.getDefault()).apply {
    timeInMillis = System.currentTimeMillis()
}.time)

val sizeFormatMap = mapOf(Pair(0, "B"), Pair(1, "KB"), Pair(2, "MB"), Pair(3, "GB"))

fun Long.getFormatStorageSize(): String {
    var i = this@getFormatStorageSize
    var times = 0
    val size = sizeFormatMap.size - 1
    while (i > 1024) {
        i /= 1024
        if (++times >= size) break
    }
    return if (times <= size) "$i${sizeFormatMap[times]}" else {
        "$i${sizeFormatMap[0]}"
    }
}

fun Long.getStorageSize(): String {
    return getSST(0)
}

private fun Long.getSST(times: Int): String {
    if (times >= sizeFormatMap.size - 1) return "$this${sizeFormatMap[0]}"
    if (this < 1024) return "$this${sizeFormatMap[times]}"
    val i = this / 1024
    var count = times
    return i.getSST(++count)
}

fun Int.getString(): String {
    return APP.app.getString(this)
}