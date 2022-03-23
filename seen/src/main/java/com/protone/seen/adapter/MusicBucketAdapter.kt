package com.protone.seen.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.TAG
import com.protone.api.context.layoutInflater
import com.protone.api.toBitmapByteArray
import com.protone.database.room.dao.MusicDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.R
import com.protone.seen.databinding.MusicBucketAdapterLayoutBinding
import java.io.ByteArrayOutputStream


class MusicBucketAdapter(context: Context, var musicBuckets: MutableList<MusicBucket>) :
    SelectListAdapter<MusicBucketAdapterLayoutBinding, MusicBucket>(context) {

    override val select: (
        holder: Holder<MusicBucketAdapterLayoutBinding>,
        isSelect: Boolean
    ) -> Unit =
        { holder, isSelect ->
            holder.binding.musicBucketBack.setBackgroundColor(
                context.resources.getColor(
                    if (isSelect) R.color.transparent_black else R.color.transparent_white,
                    context.theme
                )
            )
        }

    override fun itemIndex(path: MusicBucket): Int = musicBuckets.indexOf(path)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<MusicBucketAdapterLayoutBinding> {
        return Holder(
            MusicBucketAdapterLayoutBinding.inflate(
                context.layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: Holder<MusicBucketAdapterLayoutBinding>, position: Int) {
        holder.binding.apply {
            musicBucketName.text = musicBuckets[position].name
            musicBucketIcon.apply {
                when {
                    musicBuckets[position].Icon != null -> {
                        loadIcon(this, Uri.parse(musicBuckets[position].Icon).toBitmapByteArray())
                    }
//                    musicBuckets[position].musicList != null && musicBuckets[position].musicList?.isNotEmpty() == true -> {
//                        musicBuckets[position].musicList?.let { list ->
//                            loadIcon(
//                                this,
//                                list[list.size - 1].albumID.toBitmapByteArray()
//                            )
//                        }
//                    }
                    else -> {
                        loadIcon(
                            this,
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_baseline_music_note_24,
                                null
                            )
                        )
                    }
                }
            }
//            musicBucketNum.text = musicBuckets[position].musicList?.size?.toString() ?: "0"
        }
    }

    private fun loadIcon(imageView: ImageView, byteArray: ByteArray?) {
        Glide.with(imageView.context).load(byteArray)
            .transition(
                DrawableTransitionOptions.withCrossFade()
            ).into(imageView)
    }

    private fun loadIcon(imageView: ImageView, drawable: Drawable?) {
        Glide.with(imageView.context).load(drawable)
            .transition(
                DrawableTransitionOptions.withCrossFade()
            ).into(imageView)
    }

    override fun getItemCount(): Int = musicBuckets.size

    fun addBucket(musicBucket: MusicBucket) {
        musicBuckets.add(musicBucket)
        notifyItemInserted(musicBuckets.indexOf(musicBucket))
    }

}