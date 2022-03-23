package com.protone.api

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.protone.api.context.Global

fun String.getFileName(): String {
    return this.split("/").run {
        this[this.size - 1]
    }
}

fun String.getParentPath(): String {
    return this.substring(0, lastIndexOf("/"))
}

fun Uri.toBitmapByteArray(): ByteArray? {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    mediaMetadataRetriever.setDataSource(Global.application, this)
    return mediaMetadataRetriever.embeddedPicture
}