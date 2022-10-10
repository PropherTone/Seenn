package com.protone.api.note

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView

interface IRichNoteImageLoader {
    fun loadImage(context: Context, uri: Uri?, view: ImageView)
    fun loadImage(context: Context, path: String?, view: ImageView)
    fun loadImage(context: Context, bitmap: Bitmap?, view: ImageView)
    fun loadError(context: Context, view: ImageView)
}