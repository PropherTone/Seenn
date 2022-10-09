package com.protone.ui.customView.richText

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.protone.api.baseType.toBitmap
import com.protone.api.context.SApplication
import com.protone.ui.R
import com.protone.ui.customView.richText.note.IRichNoteImageLoader

class RichNoteImageLoader : IRichNoteImageLoader {

    override fun loadImage(context: Context, uri: Uri?, view: ImageView) {
        TODO("Not yet implemented")
    }

    override fun loadImage(context: Context, path: String?, view: ImageView) {
        val bitmapWH = getWHFromPath(path)
        Glide.with(context).asDrawable()
            .load(path).error(R.drawable.ic_baseline_error_outline_24_black)
            .let { glide ->
                if (bitmapWH != null) glide.override(bitmapWH[0], bitmapWH[1]) else glide
            }.into(view)
    }

    override fun loadImage(context: Context, bitmap: Bitmap?, view: ImageView) {
        if (bitmap == null) return
        val bitmapWH = getBitmapWH(bitmap)
        Glide.with(context).asDrawable().load(bitmap)
            .error(R.drawable.ic_baseline_error_outline_24_black).let { glide ->
                if (bitmapWH != null) glide.override(bitmapWH[0], bitmapWH[1]) else glide
            }.into(view)
    }

    override fun loadError(context: Context, view: ImageView) {
        Glide.with(context).asDrawable()
            .load(R.drawable.ic_baseline_error_outline_24_black)
            .into(view)
    }

    private fun getWHFromPath(path: String?): IntArray? {
        return try {
            val option = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val dba = path?.toBitmap(option) ?: return null
            val height = dba.height
            val width = dba.width
            val index = width / height
            val bmH = SApplication.screenWidth / index
            dba.recycle()
            intArrayOf(SApplication.screenWidth, bmH)
        } catch (e: Exception) {
            null
        }
    }

    private fun getBitmapWH(dba: Bitmap): IntArray? {
        return try {
            val height = dba.height
            val width = dba.width
            val index = width / height
            val bmH = SApplication.screenWidth / index
            intArrayOf(SApplication.screenWidth, bmH)
        } catch (e: Exception) {
            null
        }
    }

}