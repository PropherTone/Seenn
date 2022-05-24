package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.R
import com.protone.seen.databinding.MusicBucketAdapterLayoutBinding


class MusicBucketAdapter(context: Context, musicBucket: MusicBucket) :
    SelectListAdapter<MusicBucketAdapterLayoutBinding, MusicBucket>(context) {

    init {
        selectList.add(musicBucket)
    }

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
                    if (isSelect) R.color.gray_2 else R.color.white,
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
        setSelect(holder, selectList.contains(musicBuckets[position]))
        holder.binding.apply {
            musicBucketName.text = musicBuckets[position].name
            musicBucketIcon.apply {
                when {
                    musicBuckets[position].icon != null -> {
                        loadIcon(this, byteArray = musicBuckets[position].icon)
                    }
                    else -> {
                        loadIcon(
                            this,
                            drawable = ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_baseline_music_note_24,
                                null
                            )
                        )
                    }
                }
            }
            musicBucketNum.text = musicBuckets[position].size.toString()

            musicBucketBack.setOnClickListener {
                if (!selectList.contains(musicBuckets[position]))
                    checkSelect(holder, musicBuckets[position])
                clickCallback(musicBuckets[holder.layoutPosition].name)
            }

            fun closeMusicBucketBack() {
                AnimationHelper.translationX(
                    musicBucketBack,
                    -musicBucketBack.measuredWidth.toFloat(),
                    0f,
                    200,
                    play = true,
                    doOnStart = {
                        musicBucketBack.isVisible = true
                    }
                )
            }

            if (musicBuckets[holder.layoutPosition].name != context.getString(R.string.all_music)) musicBucketAction.setOnClickListener {
                when (musicBucketBack.isVisible) {
                    true -> {
                        AnimationHelper.translationX(
                            musicBucketBack,
                            0f,
                            -musicBucketBack.measuredWidth.toFloat(),
                            200,
                            play = true,
                            doOnEnd = {
                                musicBucketBack.isVisible = false
                            }
                        )
                    }
                    false -> {
                        closeMusicBucketBack()
                    }
                }
            }
            musicBucketEdit.setOnClickListener {
                musicBucketEvent?.edit(musicBuckets[holder.layoutPosition].name,position)
                closeMusicBucketBack()
            }
            musicBucketDelete.setOnClickListener {
                musicBucketEvent?.delete(musicBuckets[holder.layoutPosition].name,position)
                closeMusicBucketBack()
            }
            musicBucketAddList.setOnClickListener {
                closeMusicBucketBack()
                musicBucketEvent?.addList(musicBuckets[holder.layoutPosition].name,position)
            }
        }
    }

    var musicBucketEvent: MusicBucketEvent? = null

    interface MusicBucketEvent {
        fun addList(bucket: String,position: Int)
        fun delete(bucket: String,position: Int)
        fun edit(bucket: String,position: Int)
    }

    private fun loadIcon(
        imageView: ImageView,
        byteArray: ByteArray? = null,
        drawable: Drawable? = null
    ) {
        Glide.with(context).asDrawable().apply {
            (if (byteArray != null) load(byteArray) else load(drawable))
                .transition(DrawableTransitionOptions.withCrossFade())
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_baseline_music_note_24)
                .override(imageView.measuredWidth, imageView.measuredHeight)
                .into(imageView)
        }
    }

    override fun getItemCount(): Int = musicBuckets.size

    fun addBucket(musicBucket: MusicBucket) {
        musicBuckets.add(musicBucket)
        notifyItemInserted(musicBuckets.indexOf(musicBucket))
    }

    fun deleteBucket(musicBucket: MusicBucket): Boolean {
        val index = musicBuckets.indexOf(musicBucket)
        musicBuckets.removeAt(index)
        notifyItemRemoved(index)
        return index != -1
    }

    fun refreshBucket(name: String, bucket: MusicBucket) {
        val indexOfFirst = musicBuckets.indexOfFirst { it.name == name }
        if (indexOfFirst != -1 && indexOfFirst != 0) {
            musicBuckets[indexOfFirst] = bucket
            Log.d("TAG", "refreshBucket: $indexOfFirst")
            notifyItemChanged(indexOfFirst)
        }
    }

}