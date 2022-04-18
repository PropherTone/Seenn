package com.protone.seen

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.TAG
import com.protone.api.context.Global
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.adapter.MainModelListAdapter
import com.protone.seen.databinding.MainLayoutBinding

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
            if (value != field)
                binding.musicPlayer.name = value
            field = value
        }


    var icon: Uri = Uri.parse("")
        set(value) {
            if (value != field)
                binding.musicPlayer.icon = value
            field = value
        }

    var isPlaying: Boolean = false
        set(value) {
            if (value != field)
                binding.musicPlayer.isPlaying = value
            field = value
        }

    val group: ViewGroup
        get() = binding.mainGroup

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.mainGroup

    init {
        initToolBar()
        binding.apply {
            self = this@MainSeen
            toolbar.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    Log.d(TAG, "$verticalOffset: ${appBarLayout.totalScrollRange.toFloat()}")
                    binding.toolMotion.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat()
                    Log.d(TAG, "${binding.toolMotion.progress}: ")
                })
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

    override fun offer(event: Touch) {
        viewEvent.offer(event)
    }

    fun musicSeek(listener: Progress) {
        binding.musicPlayer.seekTo = listener
    }


}