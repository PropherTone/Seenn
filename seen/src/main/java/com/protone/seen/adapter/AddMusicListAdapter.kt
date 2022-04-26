package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.api.context.onUiThread
import com.protone.api.toStringMinuteTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Galley
import com.protone.seen.AddMusic2BucketSeen
import com.protone.seen.R
import com.protone.seen.databinding.MusicListLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AddMusicListAdapter(context: Context, private val bucket: String, private val mode: String) :
    SelectListAdapter<MusicListLayoutBinding, Music>(context) {

    init {
        multiChoose = (mode != AddMusic2BucketSeen.PICK_MUSIC).also { b->
            if (b) Galley.musicBucket[bucket]?.let { selectList.addAll(it) }
        }
    }

    var musicList = mutableListOf<Music>()
        set(value) {
            field.clear()
            field.addAll(value)
        }

    private val viewQueue = PriorityQueue<Int>()

    override val select: (holder: Holder<MusicListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.apply {
                if (isSelect) {
                    musicListPlayState.visibility = View.VISIBLE
                    musicListPlayState.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.load_animation,
                            null
                        )
                    )
                    musicListContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.zima_blue
                        )
                    )
                    musicListName.setTextColor(ContextCompat.getColor(context, R.color.white))
                    musicListTime.setTextColor(ContextCompat.getColor(context, R.color.white))
                    musicListDetail.setTextColor(ContextCompat.getColor(context, R.color.white))
                    startAnimation(musicListInContainer)
                } else {
                    musicListPlayState.visibility = View.GONE
                    musicListPlayState.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.ic_baseline_check_24,
                            null
                        )
                    )
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
        return Holder(MusicListLayoutBinding.inflate(context.layoutInflater, parent, false)).apply {
            binding.isLoad = true
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder<MusicListLayoutBinding>, position: Int) {
        holder.binding.apply {
            musicList[position].let { music ->
                setSelect(holder, selectList.contains(music))

                musicListContainer.setOnClickListener {
                    checkSelect(holder, music)
                    viewQueue.add(position)
                    musicListPlayState.drawable.let { d ->
                        when (d) {
                            is Animatable -> {
                                d.start()
                                if (mode != AddMusic2BucketSeen.PICK_MUSIC) {
                                    music.myBucket.apply {
                                        (this as ArrayList).add(bucket)
                                    }
                                    DataBaseDAOHelper.updateMusicCB(
                                        music
                                    ) { re ->
                                        if (re != -1 && re != 0) {
                                            changeIconAni(musicListPlayState)
                                        } else {
                                            selectList.remove(music)
                                            notifyItemChanged()
                                        }
                                    }
                                } else {
                                    changeIconAni(musicListPlayState)
                                }
                                d.stop()
                            }
                            else -> {
                                selectList.remove(music)
                                notifyItemChanged()
                            }
                        }
                    }
                }

                musicListName.text = music.title
                if (music.artist != null && music.album != null) {
                    musicListDetail.text = "${music.artist} · ${music.album}"
                } else musicListDetail.isGone = true
                musicListTime.text = music.duration.toStringMinuteTime()
            }
        }
    }

    private fun notifyItemChanged() {
        while (!viewQueue.isNullOrEmpty()) {
            val poll = viewQueue.poll()
            if (poll != null) {
                context.onUiThread {
                    notifyItemChanged(poll)
                }
            }
        }
    }

    private fun changeIconAni(view: ImageView) = context.onUiThread {
        AnimationHelper.apply {
            animatorSet(scaleX(view, 0f), scaleY(view, 0f), doOnEnd = {
                view.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_baseline_check_24,
                        null
                    )
                )
                animatorSet(scaleX(view, 1f), scaleY(view, 1f), play = true)
            }, play = true)
        }
    }


    override fun getItemCount(): Int = musicList.size
}