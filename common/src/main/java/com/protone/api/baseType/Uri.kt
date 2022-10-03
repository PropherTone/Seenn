package com.protone.api.baseType

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.protone.api.R
import com.protone.api.context.SApplication
import com.protone.api.isInDebug
import com.protone.api.onResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

suspend fun Uri.imageSaveToFile(
    fileName: String,
    dir: String? = null,
    w: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
) = withContext(Dispatchers.IO) {
    toBitmap(w, h)?.let {
        try {
            it.saveToFile("$fileName.png", dir)
        } finally {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
}


suspend fun Uri.imageSaveToDisk(
    fileName: String,
    dir: String? = null,
    w: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
): String? {
    var exists = false
    var hasBytes = true
    return onResult {
        val bytes = SApplication.app.contentResolver.openInputStream(this@imageSaveToDisk)
            ?.use { inputStream -> inputStream.readBytes() } ?: toBitmapByteArray()
        it.resumeWith(Result.success(if (bytes == null) {
            hasBytes = false
            null
        } else SApplication.app.filesDir.absolutePath.useAsParentDirToSaveFile(
            fileName,
            dir,
            onExists = { file ->
                if (file.getSHA() == bytes.getSHA()) {
                    true
                } else {
                    exists = true
                    true
                }
            },
            onNewFile = { file ->
                FileOutputStream(file).use { outputStream -> outputStream.write(bytes) }
                true
            }
        )))
    }.let {
        if (it == null && exists) {
            this@imageSaveToDisk.imageSaveToDisk("${fileName}_new.png", dir, w, h)
        } else if (hasBytes) {
            imageSaveToFile(fileName, dir, w, h)
        } else null
    }
}

suspend fun Uri.toBitmap(
    w: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon),
    h: Int = SApplication.app.resources.getDimensionPixelSize(R.dimen.huge_icon)
): Bitmap? = onResult {
    it.resumeWith(Result.success(
        toMediaBitmap(w, h) ?: try {
            toBitmapByteArray()?.let { byteArray ->
                BitmapFactory.decodeByteArray(
                    byteArray,
                    0,
                    byteArray.size,
                    null
                )
            }
        } catch (e: IOException) {
            null
        }))
}

fun Uri.toMediaBitmap(w: Int, h: Int): Bitmap? {
    var ois = try {
        SApplication.app.contentResolver.openInputStream(this)
    } catch (e: FileNotFoundException) {
        return null
    }
    val os = ByteArrayOutputStream()
    return try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val bitmap = BitmapFactory.decodeStream(ois, null, options)
        ois?.close()
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, w, h)
        bitmap?.recycle()
        options.inJustDecodeBounds = false
        ois = try {
            SApplication.app.contentResolver.openInputStream(this)
        } catch (e: FileNotFoundException) {
            return null
        }
        BitmapFactory.decodeStream(ois, null, options)
    } catch (e: IOException) {
        if (isInDebug()) e.printStackTrace()
        null
    } finally {
        ois?.close()
        os.close()
    }
}

fun Uri.toBitmapByteArray(): ByteArray? {
    if (this == Uri.EMPTY) return null
    val mediaMetadataRetriever = MediaMetadataRetriever()
    return try {
        mediaMetadataRetriever.run {
            setDataSource(SApplication.app, this@toBitmapByteArray)
            embeddedPicture
        }
    } catch (e: IllegalArgumentException) {
        if (isInDebug()) e.printStackTrace()
        null
    } catch (e: SecurityException) {
        if (isInDebug()) e.printStackTrace()
        null
    } catch (e: RuntimeException) {
        if (isInDebug()) e.printStackTrace()
        null
    } finally {
        mediaMetadataRetriever.release()
    }
}