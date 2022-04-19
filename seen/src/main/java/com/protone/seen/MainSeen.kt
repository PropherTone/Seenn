package com.protone.seen

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.TAG
import com.protone.api.context.Global
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.todayTime
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

    var userName: String = ""
        set(value) {
            binding.userWelcome.text =
                String.format(context.getString(R.string.welcome_msg), value)
            binding.userDate.text = todayTime
            field = value
        }
        get() = binding.userWelcome.text.toString()

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
        setNavigation()
        binding.apply {
            self = this@MainSeen
            toolbar.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    binding.toolMotion.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat().also {
                            binding.musicPlayer.isVisible = it > 0.7f
                        }
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