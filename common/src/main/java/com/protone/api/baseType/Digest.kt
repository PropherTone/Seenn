package com.protone.api.baseType

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

fun ByteArray.getSHA():String?{
    return try {
        MessageDigest.getInstance("SHA").let {
            String(it.digest(this))
        }
    } catch (e: Exception) {
        null
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

fun File.getSHA(): String? {
    var fis: FileInputStream? = null
    return try {
        MessageDigest.getInstance("SHA").let {
            fis = FileInputStream(this)
            val bytes = fis?.readBytes()
            if (bytes != null) {
                String(it.digest(bytes))
            } else null
        }
    } catch (e: Exception) {
        fis?.close()
        null
    } finally {
        try {
            fis?.close()
        } catch (e: IOException) {
        }
    }
}

fun File.getMD5(): String? {
    var fis: FileInputStream? = null
    return try {
        MessageDigest.getInstance("MD5").let {
            fis = FileInputStream(this)
            val bytes = fis?.readBytes()
            if (bytes != null) {
                String(it.digest(bytes))
            } else null
        }
    } catch (e: Exception) {
        fis?.close()
        null
    } finally {
        try {
            fis?.close()
        } catch (e: IOException) {
        }
    }
}