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
    w: Int = 0,
    h: Int = 0
) = withIOContext {
    if (this == Uri.EMPTY) return@withIOContext null
    toBitmap(w, h)?.let {
        try {
            it.saveToFile(fileName, dir)
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
    w: Int = 0,
    h: Int = 0
): String? {
    if (this == Uri.EMPTY) return null
    var exists = false
    var mimeType = ""
    return onResult {
        val bytes = SApplication.app.contentResolver.openInputStream(this@imageSaveToDisk)
            ?.use { inputStream -> inputStream.readBytes() } ?: toBitmapByteArray()
        it.resumeWith(Result.success(if (bytes == null) {
            null
        } else SApplication.app.filesDir.absolutePath.useAsParentDirToSaveFile(
            bytes.getMediaMimeType().let { mime ->
                mimeType = mime
                "$fileName.$mimeType"
            },
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
            this@imageSaveToDisk.imageSaveToDisk("${fileName}_new", dir, w, h)
        } else it
    }
}

suspend fun Uri.toBitmap(
    w: Int = 0,
    h: Int = 0
): Bitmap? = onResult {
    if (this != Uri.EMPTY) it.resumeWith(Result.success(
        toMediaBitmap(w, h) ?: try {
            toBitmapByteArray()?.let { byteArray ->
                val options = BitmapFactory.Options()
                if (w != 0 && h != 0) {
                    options.inJustDecodeBounds = true
                    val bitmap = BitmapFactory.decodeByteArray(
                        byteArray,
                        0,
                        byteArray.size,
                        options
                    )
                    options.inSampleSize =
                        calculateInSampleSize(options.outWidth, options.outHeight, w, h)
                    bitmap.recycle()
                    options.inJustDecodeBounds = false
                }
                BitmapFactory.decodeByteArray(
                    byteArray,
                    0,
                    byteArray.size,
                    options
                )
            }
        } catch (e: IOException) {
            null
        }))
}

fun Uri.toMediaBitmap(w: Int, h: Int): Bitmap? {
    if (this == Uri.EMPTY) return null
    var ois = try {
        SApplication.app.contentResolver.openInputStream(this)
    } catch (e: FileNotFoundException) {
        return null
    }
    val os = ByteArrayOutputStream()
    return try {
        val options = BitmapFactory.Options()
        if (w != 0 && h != 0) {
            options.inJustDecodeBounds = true
            val bitmap = BitmapFactory.decodeStream(ois, null, options)
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, w, h)
            bitmap?.recycle()
            ois?.close()
            ois = SApplication.app.contentResolver.openInputStream(this)
            options.inJustDecodeBounds = false
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