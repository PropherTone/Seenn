package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.api.toStringMinuteTime
import com.protone.database.room.entity.Music
import com.protone.seen.R
import com.protone.seen.databinding.MusicListLayoutBinding

class MusicListAdapter(context: Context) :
    SelectListAdapter<MusicListLayoutBinding, Music>(context) {

    var musicList: MutableList<Music> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field.clear()
            field.addAll(value)
            notifyDataSetChanged()
        }

    var clickCallback: (Int) -> Unit? = {}

    private var playPosition = 0

    override val select: (holder: Holder<MusicListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.apply {
                if (isSelect) {
                    musicListContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.zima_blue
                        )
                    )
                    musicListPlayState.visibility = View.VISIBLE
                    musicListName.setTextColor(ContextCompat.getColor(context, R.color.white))
                    musicListTime.setTextColor(ContextCompat.getColor(context, R.color.white))
                    musicListDetail.setTextColor(ContextCompat.getColor(context, R.color.white))
                    startAnimation(musicListInContainer)
                } else {
                    musicListContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    musicListPlayState.visibility = View.GONE
                    musicListName.setTextColor(ContextCompat.getColor(context, R.color.black))
                    musicListTime.setTextColor(ContextCompat.getColor(context, R.color.black))
                    musicListDetail.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
            }
        }

    private fun startAnimation(target: ViewGroup) {
        AnimationHelper.apply {
            val x = scaleX(target, 0.96f, duration = 50)
            val y = scaleY(target, 0.96f, duration = 50)
            val x1 = scaleX(target, 1f, duration = 360)
            val y1 = scaleY(target, 1f, duration = 360)
            animatorSet(x, y, play = true, doOnEnd = {
                animatorSet(x1, y1, play = true)
            })
        }
    }

    override fun itemIndex(path: Music): Int = musicList.indexOf(path)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<MusicListLayoutBinding> {
        return Holder(
            MusicListLayoutBinding.inflate(
                context.layoutInflater,
                parent,
                false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder<MusicListLayoutBinding>, position: Int) {
        holder.binding.apply {
            musicList[position].let { music ->
                setSelect(holder, selectList.contains(music))
                musicListContainer.setOnClickListener {
                    if (playPosition == holder.layoutPosition) return@setOnClickListener
                    checkSelect(holder, music)
                    playPosition = holder.layoutPosition
                    clickCallback(holder.layoutPosition)
                }
                musicListName.text = music.title
                musicListDetail.text = "${music.artist} · ${music.album}"
                musicListTime.text = music.duration.toStringMinuteTime()
            }
        }
    }

    override fun getItemCount(): Int = musicList.size

    fun playPosition(position: Int) {
        if (musicList.size <= 0) return
        musicList[position].let {
            selectList.clear()
            selectList.add(it)
            notifyItemChanged(playPosition)
            playPosition = position
            notifyItemChanged(playPosition)
        }
    }

}