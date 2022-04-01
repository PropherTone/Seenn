package com.protone.seen.adapter

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.databinding.PictureBoxAdapterLayoutBinding
import java.net.URL
import kotlin.math.roundToInt

class PictureBoxAdapter(context: Context, private val picUri: MutableList<GalleyMedia>) :
    BaseAdapter<PictureBoxAdapterLayoutBinding>(context) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<PictureBoxAdapterLayoutBinding> {
        val binding = PictureBoxAdapterLayoutBinding
            .inflate(LayoutInflater.from(context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<PictureBoxAdapterLayoutBinding>, position: Int) {
        holder.binding.imageView.let { image ->
            image.scaleType = ImageView.ScaleType.FIT_XY
            Glide.with(image.context).load(picUri[position].uri)
                .addListener(object : RequestListener<Drawable>{
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.apply {
                            val mix = this.intrinsicWidth.toFloat().let {
                                image.width / it
                            }
                            val heightSpan = (this.intrinsicHeight * mix).roundToInt()
                            image.updateLayoutParams {
                                this.height = heightSpan
                            }
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                })
                .into(image)
        }
    }


    override fun getItemCount(): Int {
        return picUri.size
    }
}