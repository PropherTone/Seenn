package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.R
import com.protone.seen.databinding.MusicBucketAdapterLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
                        loadIcon(this, musicBuckets[position].icon)
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

            musicBucketBack.setOnClickListener {
                if (!selectList.contains(musicBuckets[position]))
                    checkSelect(holder, musicBuckets[position])
                clickCallback(musicBuckets[holder.layoutPosition].name)
            }
            if (musicBuckets[holder.layoutPosition].name != context.getString(R.string.all_music))
                musicBucketAction.setOnClickListener {
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
                    }
                }
            musicBucketEdit.setOnClickListener { }
            musicBucketDelete.setOnClickListener { }
            musicBucketAddList.setOnClickListener {
                addList(musicBuckets[holder.layoutPosition].name, addList)
            }
        }
    }

    var addList: (String) -> Unit = {}
    private inline fun addList(bucket: String, crossinline onClick: (String) -> Unit) =
        onClick(bucket)

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