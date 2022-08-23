package com.protone.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.getDrawable
import com.protone.api.baseType.getString
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.MusicBucket
import com.protone.ui.R
import com.protone.ui.databinding.MusicBucketAdapterLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MusicBucketAdapter(context: Context, musicBucket: MusicBucket) :
    SelectListAdapter<MusicBucketAdapterLayoutBinding, MusicBucket, MusicBucketAdapter.MusicBucketAEvent>(
        context,
        true
    ) {

    sealed class MusicBucketAEvent {
        data class AddBucket(val musicBucket: MusicBucket) : MusicBucketAEvent()
        data class RefreshBucket(val name: String, val bucket: MusicBucket) : MusicBucketAEvent()
    }

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

    var musicBucketEvent: MusicBucketEvent? = null

    var clickCallback: ((String) -> Unit)? = null

    override suspend fun onEventIO(data: MusicBucketAEvent) {
        when (data) {
            is MusicBucketAEvent.AddBucket -> {
                musicBuckets.add(data.musicBucket)
                val index = musicBuckets.indexOf(data.musicBucket)
                if (index != -1) {
                    withContext(Dispatchers.IO) {
                        notifyItemInserted(index)
                    }
                }
            }
            is MusicBucketAEvent.RefreshBucket -> {
                val index = musicBuckets.indexOfFirst { it.name == data.name }
                if (index != -1 && index != 0) {
                    musicBuckets[index] = data.bucket
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(index)
                    }
                }
            }
        }
    }

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
                context.newLayoutInflater,
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
                        loadIcon(this, iconPath = musicBuckets[position].icon)
                    }
                    else -> {
                        loadIcon(
                            this,
                            drawable = R.drawable.ic_baseline_music_note_24.getDrawable()
                        )
                    }
                }
            }
            musicBucketNum.text = musicBuckets[position].size.toString()

            musicBucketBack.setOnClickListener {
                if (!selectList.contains(musicBuckets[position]))
                    checkSelect(holder, musicBuckets[position])
                clickCallback?.invoke(musicBuckets[holder.layoutPosition].name)
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

            if (musicBuckets[holder.layoutPosition].name != R.string.all_music.getString()) musicBucketAction.setOnClickListener {
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
                musicBucketEvent?.edit(musicBuckets[holder.layoutPosition].name, position)
                closeMusicBucketBack()
            }
            musicBucketDelete.setOnClickListener {
                musicBucketEvent?.delete(musicBuckets[holder.layoutPosition].name, position)
                closeMusicBucketBack()
            }
            musicBucketAddList.setOnClickListener {
                closeMusicBucketBack()
                musicBucketEvent?.addList(musicBuckets[holder.layoutPosition].name, position)
            }
        }
    }

    private fun loadIcon(
        imageView: ImageView,
        iconPath: String? = null,
        drawable: Drawable? = null
    ) {
        Glide.with(context).asDrawable().apply {
            (if (iconPath != null) load(iconPath) else load(drawable))
                .transition(DrawableTransitionOptions.withCrossFade())
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_baseline_music_note_24)
                .override(imageView.measuredWidth, imageView.measuredHeight)
                .into(imageView)
        }
    }

    override fun getItemCount(): Int = musicBuckets.size

    fun deleteBucket(musicBucket: MusicBucket): Boolean {
        val index = musicBuckets.indexOf(musicBucket)
        musicBuckets.removeAt(index)
        selectList.clear()
        selectList.add(musicBuckets[0])
        notifyItemRemoved(index)
        return index != -1
    }

    fun addBucket(musicBucket: MusicBucket) {
       emit(MusicBucketAEvent.AddBucket(musicBucket))
    }

    fun refreshBucket(name: String, bucket: MusicBucket) {
        emit(MusicBucketAEvent.RefreshBucket(name, bucket))
    }

    interface MusicBucketEvent {
        fun addList(bucket: String, position: Int)
        fun delete(bucket: String, position: Int)
        fun edit(bucket: String, position: Int)
    }
}