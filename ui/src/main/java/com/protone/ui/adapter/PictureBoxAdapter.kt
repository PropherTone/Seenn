package com.protone.ui.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.databinding.ViewDataBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.protone.api.entity.GalleryMedia
import com.protone.ui.databinding.PictureBoxAdapterGifLayoutBinding
import com.protone.ui.databinding.PictureBoxAdapterLayoutBinding
import com.protone.ui.databinding.PictureBoxAdapterVideoLayoutBinding
import kotlin.math.roundToInt

class PictureBoxAdapter(context: Context, private val picUri: MutableList<GalleryMedia>) :
    BaseAdapter<ViewDataBinding, Any>(context) {

    private val image = 0
    private val video = 1
    private val gif = 3

    override fun getItemViewType(position: Int): Int {
        return if (picUri[position].name.contains("gif")) gif else if (picUri[position].isVideo) video else image
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<ViewDataBinding> {
        val binding: ViewDataBinding = when (viewType) {
            video -> PictureBoxAdapterVideoLayoutBinding
                .inflate(LayoutInflater.from(context), parent, false)
            gif -> PictureBoxAdapterGifLayoutBinding
                .inflate(LayoutInflater.from(context), parent, false)
            else -> PictureBoxAdapterLayoutBinding
                .inflate(LayoutInflater.from(context), parent, false)
        }
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<ViewDataBinding>, position: Int) {
        when (holder.binding) {
            is PictureBoxAdapterGifLayoutBinding -> holder.binding.apply {
                image.scaleType = ImageView.ScaleType.FIT_XY
                Glide.with(context).load(picUri[position].uri)
                    .addListener(object : RequestListener<Drawable> {
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

                    }).into(image)
            }
            is PictureBoxAdapterLayoutBinding -> holder.binding.apply {
                image.setImageResource(picUri[position].uri)
            }
            is PictureBoxAdapterVideoLayoutBinding -> holder.binding.apply {
                start.isGone = false
                videoCover.isGone = false
                Glide.with(context).load(picUri[position].path)
                    .addListener(object : RequestListener<Drawable> {
                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            resource?.apply {
                                val mix = this.intrinsicWidth.toFloat().let {
                                    videoCover.width / it
                                }
                                val heightSpan = (this.intrinsicHeight * mix).roundToInt()
                                videoCover.updateLayoutParams {
                                    this.height = heightSpan
                                }
                                videoPlayer.updateLayoutParams {
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

                    }).into(videoCover)
                start.setOnClickListener {
                    start.isGone = true
                    videoCover.isGone = true
                    videoPlayer.setVideoPath(picUri[holder.layoutPosition].uri)
                }
                videoPlayer.doOnCompletion {
                    videoPlayer.release()
                    start.isGone = false
                    videoCover.isGone = false
                }
            }
        }
    }

    override fun onViewRecycled(holder: Holder<ViewDataBinding>) {
        when (holder.binding) {
            is PictureBoxAdapterLayoutBinding -> holder.binding.apply {
                image.clear()
            }
            is PictureBoxAdapterVideoLayoutBinding -> holder.binding.apply {
                start.isGone = false
                videoCover.isGone = false
                videoPlayer.release()
            }
        }
        super.onViewRecycled(holder)
    }


    override fun getItemCount(): Int {
        return picUri.size
    }
}