package com.protone.api.baseType

import android.graphics.Bitmap
import android.graphics.Matrix
import com.protone.api.context.SApplication
import com.protone.api.isInDebug
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Bitmap.saveToFile(fileName: String, dir: String? = null): String? {
    var fileOutputStream: FileOutputStream? = null
    return try {
        val tempPath =
            if (dir != null) {
                val dirFile = File("${SApplication.app.filesDir.absolutePath}/$dir/")
                if (!dirFile.exists() && !dirFile.mkdirs()) {
                    "图片${fileName}.png保存失败!".toast()
                    return null
                }
                "${SApplication.app.filesDir.absolutePath}/$dir/${fileName.getFileName()}.png"
            } else "${SApplication.app.filesDir.absolutePath}/${fileName.getFileName()}.png"
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

fun calculateInSampleSize(bitmap: Bitmap, h: Int, w: Int): Int {
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
