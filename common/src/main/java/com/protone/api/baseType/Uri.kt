package com.protone.api.baseType

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.protone.api.R
import com.protone.api.context.SApplication
import com.protone.api.isInDebug
import java.io.ByteArrayOutputStream
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
        ois = SApplication.app.contentResolver.openInputStream(this) ?: return null
        val bitmap = BitmapFactory.decodeStream(ois, null, null)
        if (bitmap != null) {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                getMatrix(bitmap.height, bitmap.width, w),
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
    val ois = SApplication.app.contentResolver.openInputStream(this) ?: return byteArray
    val os = ByteArrayOutputStream()
    try {
        var options = BitmapFactory.Options()
        val dimensionPixelSize =
            SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
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
        } else return null
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
        mediaMetadataRetriever.setDataSource(SApplication.app, this)
        embeddedPicture = mediaMetadataRetriever.embeddedPicture
    } catch (e: Exception) {
        if (isInDebug()) e.printStackTrace()
    } finally {
        mediaMetadataRetriever.release()
    }
    return embeddedPicture
}