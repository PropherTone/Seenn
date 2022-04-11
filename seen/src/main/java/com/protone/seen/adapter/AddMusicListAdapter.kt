package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.protone.api.TAG
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.api.toStringMinuteTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Music
import com.protone.seen.R
import com.protone.seen.databinding.MusicListLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class AddMusicListAdapter(context: Context, private val bucket: String) :
    SelectListAdapter<MusicListLayoutBinding, Music>(context) {

    init {
        multiChoose = true
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
                    musicListContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_9
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
                    if (selectList.contains(music)) return@setOnClickListener
                    checkSelect(holder, music)
                    viewQueue.add(position)
                    musicListPlayState.drawable.let { d ->
                        when (d) {
                            is Animatable -> {
                                d.start()
                                DataBaseDAOHelper.updateMusicMyBucketCB(
                                    music.title,
                                    (music.myBucket ?: arrayListOf()).also { bs ->
                                        (bs as ArrayList).add(bucket)
                                    }
                                ) { re ->
                                    if (re != -1 && re != 0) {
                                        changeIconAni(musicListPlayState)
                                    } else {
                                        selectList.remove(music)
                                        notifyItemChanged()
                                    }
                                    d.stop()
                                }
                            }
                            else -> {
                                selectList.remove(music)
                                notifyItemChanged()
                            }
                        }
                    }
                }

                musicListName.text = music.title
                musicListDetail.text = "${music.artist} · ${music.album}"
                musicListTime.text = music.duration.toStringMinuteTime()
            }
        }
    }

    private fun notifyItemChanged() {
        while (!viewQueue.isNullOrEmpty()) {
            val poll = viewQueue.poll()
            if (poll != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    notifyItemChanged(poll)
                }
            }
        }
    }

    private fun changeIconAni(view: ImageView) = CoroutineScope(Dispatchers.Main).launch {
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