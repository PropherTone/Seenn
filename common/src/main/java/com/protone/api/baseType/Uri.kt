package com.protone.api.baseType

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.protone.api.R
import com.protone.api.context.SApplication
import com.protone.api.isInDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

fun Uri.saveToFile(
    fileName: String,
    dir: String? = null,
    w: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
) = toBitmap(w, h)?.saveToFile(fileName, dir)

fun Uri.toBitmap(
    w: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
): Bitmap? {
    var ois: InputStream? = null
    return try {
        ois = SApplication.app.contentResolver.openInputStream(this)
        val bitmap = BitmapFactory.decodeStream(ois, null, null) ?: (toMediaBitmapByteArray()
            ?: toBitmapByteArray())?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size, null)
        } ?: return null
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            getMatrix(bitmap.height, bitmap.width, w),
            true
        ).let {
            bitmap.recycle()
            it
        }
    } catch (e: IOException) {
        null
    } finally {
        ois?.close()
    }
}

suspend fun Uri.toByteArray(): ByteArray? = withContext(Dispatchers.IO) {
    toMediaBitmapByteArray() ?: toBitmapByteArray()
}

fun Uri.toMediaBitmapByteArray(): ByteArray? {
    var byteArray: ByteArray? = null
    var ois = try {
        SApplication.app.contentResolver.openInputStream(this)
    } catch (e: FileNotFoundException) {
        return byteArray
    }
    val os = ByteArrayOutputStream()
    try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(ois, null, options)
        ois?.close()
        val dimensionPixelSize =
            SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
        options.inSampleSize = calculateInSampleSize(
            options.outWidth,
            options.outHeight,
            dimensionPixelSize,
            dimensionPixelSize
        )
        options.inJustDecodeBounds = false
        ois = try {
            SApplication.app.contentResolver.openInputStream(this)
        } catch (e: FileNotFoundException) {
            return byteArray
        }
        BitmapFactory.decodeStream(ois, null, options)
            ?.compress(Bitmap.CompressFormat.PNG, 100, os)
        byteArray = os.toByteArray().let {
            if (it.isEmpty()) return null
            it
        }
    } catch (e: IOException) {
        if (isInDebug()) e.printStackTrace()
    } finally {
        ois?.close()
        os.close()
    }
    return byteArray
}

fun Uri.toBitmapByteArray(): ByteArray? {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    var embeddedPicture: ByteArray? = null
    try {
        mediaMetadataRetriever.setDataSource(SApplication.app, this)
        embeddedPicture = mediaMetadataRetriever.embeddedPicture
    } catch (e: IllegalArgumentException) {
        if (isInDebug()) e.printStackTrace()
    } catch (e: SecurityException) {
        if (isInDebug()) e.printStackTrace()
    } catch (e: RuntimeException) {
        if (isInDebug()) e.printStackTrace()
    } finally {
        mediaMetadataRetriever.release()
    }
    return embeddedPicture
}