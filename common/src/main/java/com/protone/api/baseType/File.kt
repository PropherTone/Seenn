package com.protone.api.baseType

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

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