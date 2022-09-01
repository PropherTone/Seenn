package com.protone.api.baseType

import android.graphics.Bitmap
import android.graphics.Matrix
import com.protone.api.context.SApplication
import com.protone.api.isInDebug
import java.io.*
import java.security.MessageDigest

fun Bitmap.saveToFile(fileName: String, dir: String? = null): String? {
    var fileOutputStream: FileOutputStream? = null
    fun saveFailed(fileName: String): String? {
        "图片${fileName}.png保存失败!".toast()
        return null
    }
    return try {
        val tempPath =
            if (dir != null) {
                val dirFile = File("${SApplication.app.filesDir.absolutePath}/$dir/")
                if (!dirFile.exists() && !dirFile.mkdirs()) {
                    return saveFailed(fileName)
                }
                "${SApplication.app.filesDir.absolutePath}/$dir/$fileName.png"
            } else "${SApplication.app.filesDir.absolutePath}/$fileName.png"
        val file = File(tempPath)
        when {
            file.exists() -> {
                if (file.getSHA() == this.getSHA()) {
                    tempPath
                } else {
                    saveToFile("${fileName}_new", dir)
                }
            }
            file.createNewFile() -> {
                fileOutputStream = FileOutputStream(file)
                this@saveToFile.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                tempPath
            }
            else -> {
                saveFailed(fileName)
            }
        }
    } catch (e: IOException) {
        saveFailed(fileName)
    } catch (e: FileNotFoundException) {
        saveFailed(fileName)
    } finally {
        try {
            fileOutputStream?.close()
        } catch (e: IOException) {
            if (isInDebug()) e.printStackTrace()
        }
    }
}

fun Bitmap.getSHA(): String? {
    return try {
        MessageDigest.getInstance("SHA").let {
            val bos = ByteArrayOutputStream()
            this.compress(Bitmap.CompressFormat.PNG, 100, bos)
            String(it.digest(bos.toByteArray()))
        }
    } catch (e: Exception) {
        null
    }
}

fun getMatrix(h: Int, w: Int, output: Int): Matrix {
    val matrix = Matrix()
    var scale = 1f
    val revers: Boolean
    val shorterLen = if (h > w) {
        revers = true
        w
    } else {
        revers = false
        h
    }

    if (output < shorterLen) {
        scale = output.toFloat() / shorterLen
    }

    if (revers) {
        matrix.setScale(scale, scale)
    } else {
        matrix.setScale(scale, scale)
    }
    return matrix
}

fun calculateInSampleSize(outWidth: Int, outHeight: Int, h: Int, w: Int): Int {
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
