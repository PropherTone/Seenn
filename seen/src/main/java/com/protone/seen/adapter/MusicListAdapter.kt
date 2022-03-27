package com.protone.seen.adapter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.protone.api.context.layoutInflater
import com.protone.api.context.toStringMinuteTime
import com.protone.database.room.entity.Music
import com.protone.seen.R
import com.protone.seen.databinding.MusicListLayoutBinding

class MusicListAdapter(context: Context) :
    SelectListAdapter<MusicListLayoutBinding, Music>(context) {

    var musicList = mutableListOf<Music>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field.clear()
            field.addAll(value)
            notifyDataSetChanged()
        }

    override val select: (holder: Holder<MusicListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.apply {
                if (isSelect) {
                    musicListPlayState.visibility = View.VISIBLE
                    musicListContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_1
                        )
                    )
                    musicListName.setTextColor(ContextCompat.getColor(context, R.color.white))
                    musicListTime.setTextColor(ContextCompat.getColor(context, R.color.white))
                    musicListDetail.setTextColor(ContextCompat.getColor(context, R.color.white))
                    startAnimation(musicListInContainer)
                } else {
                    musicListPlayState.visibility = View.GONE
                    musicListContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    musicListName.setTextColor(ContextCompat.getColor(context, R.color.black))
                    musicListTime.setTextColor(ContextCompat.getColor(context, R.color.black))
                    musicListDetail.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
            }
        }

    private fun startAnimation(target: ViewGroup) {
        val x = ObjectAnimator.ofFloat(target, "scaleX", 0.96f).apply { duration = 50 }
        val y = ObjectAnimator.ofFloat(target, "scaleY", 0.96f).apply { duration = 50 }
        val x1 = ObjectAnimator.ofFloat(target, "scaleX", 1f).apply { duration = 360 }
        val y2 = ObjectAnimator.ofFloat(target, "scaleY", 1f).apply { duration = 360 }
        AnimatorSet().apply {
            playTogether(x, y)
            start()
            doOnEnd {
                AnimatorSet().apply {
                    playTogether(x1, y2)
                    start()
                }
            }
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
                    checkSelect(holder, music)
                }
                musicListName.text = music.displayName
                musicListDetail.text = "${music.artist} · ${music.album}"
                musicListTime.text = music.duration.toStringMinuteTime()
            }
        }
    }

    override fun getItemCount(): Int = musicList.size
}