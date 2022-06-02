package com.protone.seenn.viewModel

import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import com.protone.api.context.MUSIC_NEXT
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.MUSIC_PREVIOUS
import com.protone.database.room.entity.Music
import com.protone.seen.customView.ColorfulProgressBar
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer
import com.protone.seenn.broadcast.musicBroadCastManager
import com.protone.seenn.service.MusicService

class MusicControllerIMP(private val controller: BaseMusicPlayer) {
    var binder: MusicService.MusicBinder? = null
    var loopMode = 1

    fun setBinder(lifecycle: LifecycleOwner, binder: MusicService.MusicBinder) {
        this.binder = binder
        controller.apply {
            control.setOnClickListener {
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_PLAY))
            }
            next?.setOnClickListener {
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_NEXT))
            }
            previous?.setOnClickListener {
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_PREVIOUS))
            }
            looper?.setOnClickListener {
                if (loopMode == 4) loopMode = 0
                this@MusicControllerIMP.binder?.setLoopMode(++loopMode)
            }
            this@MusicControllerIMP.binder?.run {
                refresh()
                if (progress != null) {
                    progress?.progressListener = object : ColorfulProgressBar.Progress {
                        override fun getProgress(position: Long) {
                            setProgress(position)
                        }
                    }
                    onProgress().observe(lifecycle) {
                        progress?.barSeekTo(it)
                    }
                }
                onPlayState().observe(lifecycle) {
                    isPlay = it
                }
                onMusicPlaying().observe(lifecycle) {
                    setDetail(it)
                }
            }
        }
    }

    private fun setDetail(it: Music) {
        controller.apply {
            cover = it.uri
            duration = it.duration
            setName(it.title)
            setDetail("${it.artist ?: "ARTIST"}·${it.album ?: "NONE"}")
        }
    }

    fun refresh() {
        binder?.apply {
            controller.apply {
                if (progress != null) {
                    onProgress().value?.let {
                        progress?.barSeekTo(it)
                    }
                }
                onPlayState().value?.let {
                    isPlay = it
                }
                onMusicPlaying().value?.let {
                    setDetail(it)
                }
            }
        }
    }

    fun onClick(func: () -> Unit) {
        controller.root.setOnClickListener {
            func.invoke()
        }
    }

    fun refresh(music: Music, progress: Long) = binder?.init(music, progress)

    fun getPlayList(): MutableList<Music>? = binder?.getPlayList()

    fun play(music: Music?) = binder?.play(music)

    fun setMusicList(mutableList: MutableList<Music>) = binder?.setPlayList(mutableList)

    fun getPlayingMusic() = binder?.onMusicPlaying()?.value

}