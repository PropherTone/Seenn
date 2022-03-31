package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.context.layoutInflater
import com.protone.api.toBitmapByteArray
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.R
import com.protone.seen.databinding.MusicBucketAdapterLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MusicBucketAdapter(context: Context) :
    SelectListAdapter<MusicBucketAdapterLayoutBinding, MusicBucket>(context) {

    var musicBuckets: MutableList<MusicBucket> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field.clear()
            field.addAll(value)
            notifyDataSetChanged()
        }

    var clickCallback: (String) -> Unit? = { }

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
            setSelect(holder, selectList.contains(musicBuckets[position]))
            musicBucketCard.setOnClickListener {
                clickCallback(musicBuckets[holder.layoutPosition].name)
            }
            musicBucketName.text = musicBuckets[position].name
            musicBucketIcon.apply {
                when {
                    musicBuckets[position].icon != null -> {
                        loadIcon(this, musicBuckets[position].icon?.toBitmapByteArray())
                    }
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
            musicBucketNum.text = musicBuckets[position].size.toString()
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

    suspend fun addBucket(musicBucket: MusicBucket) = withContext(Dispatchers.Main) {
        musicBuckets.add(musicBucket)
        notifyItemInserted(musicBuckets.indexOf(musicBucket))
    }

}