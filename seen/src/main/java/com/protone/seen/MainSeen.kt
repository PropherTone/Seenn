package com.protone.seen

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.protone.api.Config
import com.protone.api.TAG
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.context.to16to9Height
import com.protone.seen.adapter.MainModelListAdapter
import com.protone.seen.customView.DragBar.DragBar
import com.protone.seen.databinding.MainLayoutBinding
import java.util.*

class MainSeen(context: Context) : Seen<MainSeen.Touch>(context) {

    enum class Touch {
        MUSIC,
        NOTE,
        GALLEY,
        PlayMusic,
        PauseMusic,
        NextMusic,
        PreviousMusic,
        PauseVideo
    }

    //    val mainTheme by lazy { MainThemeStore() }
    private val binding = MainLayoutBinding.inflate(context.layoutInflater, context.root, false)
    var progress: Long = 0
        set(value) {
            binding.musicPlayer.progress = value
            field = value
        }

    var duration: Long = 0
        set(value) {
            binding.musicPlayer.duration = value
            field = value
        }

    var musicName: String = ""
        set(value) {
            binding.musicPlayer.name = value
            field = value
        }


    var icon: Uri = Uri.parse("")
        set(value) {
            binding.musicPlayer.icon = value
            field = value
        }

    var isPlaying: Boolean = false
        set(value) {
            binding.musicPlayer.isPlaying = value
            field = value
        }

    val group: ViewGroup
        get() = binding.mainGroup

    override val viewRoot: View
        get() = binding.root

    init {
        binding.apply {
            self = this@MainSeen
            musicPlayer.apply {
                duration
                playMusic = {
                    offer(Touch.PlayMusic)
                }
                pauseMusic = {
                    offer(Touch.PauseMusic)
                }
                musicPrevious = {
                    offer(Touch.PreviousMusic)
                }
                musicNext = {
                    offer(Touch.NextMusic)
                }
            }
            modelList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = MainModelListAdapter(context)
            }
        }

    }

    fun offer(msg: Touch) {
        viewEvent.offer(msg)
    }

    fun musicSeek(listener: Progress) {
        binding.musicPlayer.seekTo = listener
    }

    fun setBackGround(dr: Drawable) {
        binding.model.background = dr
    }
}