package com.protone.api

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.protone.api.context.Global
import com.protone.api.context.onBackground
import com.protone.api.context.onUiThread
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

fun String.getFileName(): String {
    return this.split("/").run { this[this.size - 1] }
}

fun String.getFileMimeType(): String {
    return "." + this.split(".").let { it[it.size - 1] }
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

fun Bitmap.saveToFile(fileName: String): String? {
    val fileOutputStream: FileOutputStream?
    return try {
        val tempPath =
            "${Global.application.filesDir.absolutePath}/${fileName.getFileName()}.png"
        val file = File(tempPath)
        when {
            file.exists() -> tempPath
            file.createNewFile() -> {
                fileOutputStream = FileOutputStream(file)
                this@saveToFile.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                tempPath
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun Uri.toBitmap(
    w: Int = Global.application.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = Global.application.resources.getDimensionPixelSize(R.dimen.huge_icon)
): Bitmap? {
    var ois: InputStream? = null
    return try {
        ois = Global.application.contentResolver.openInputStream(this) ?: return null
        BitmapFactory.decodeStream(ois, null, BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(this, h, w)
        })
    } catch (e: Exception) {
        null
    } finally {
        ois?.close()
    }
}

fun Uri.toMediaBitmapByteArray(): ByteArray? {
    var byteArray: ByteArray? = null
    val ois = Global.application.contentResolver.openInputStream(this) ?: return byteArray
    val os = ByteArrayOutputStream()
    try {
        val options = BitmapFactory.Options()
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

fun isInDebug(): Boolean {
    return try {
        val info: ApplicationInfo = Global.application.applicationInfo
        (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun Uri.toBitmapByteArray(): ByteArray? {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    var embeddedPicture: ByteArray? = null
    try {
        mediaMetadataRetriever.setDataSource(Global.application, this)
        embeddedPicture = mediaMetadataRetriever.embeddedPicture
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