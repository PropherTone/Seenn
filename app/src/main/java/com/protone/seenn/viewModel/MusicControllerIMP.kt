package com.protone.seenn.viewModel

import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.protone.api.context.MUSIC_NEXT
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.MUSIC_PREVIOUS
import com.protone.database.room.entity.Music
import com.protone.seen.R
import com.protone.seen.customView.ColorfulProgressBar
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer
import com.protone.seenn.broadcast.musicBroadCastManager
import com.protone.seenn.service.MusicService
import kotlinx.coroutines.cancel

class MusicControllerIMP(private val controller: BaseMusicPlayer) {
    var binder: MusicService.MusicBinder? = null

    companion object {
        const val LOOP_SINGLE = 0
        const val LOOP_LIST = 1
        const val PLAY_LIST = 2
        const val NO_LOOP = 3
        const val RANDOM = 4
    }

    var loop = 1

    fun setBinder(
        lifecycle: LifecycleOwner,
        binder: MusicService.MusicBinder,
        onLoop: ((Int) -> Unit)? = null
    ) {
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
                if (loop == 4) loop = 0
                this@MusicControllerIMP.binder?.setLoopMode(++loop)
                setLoopMode(loop)
                onLoop?.invoke(loop)
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

    fun setLoopMode(mode: Int) {
        loop = mode
        when (mode) {
            LOOP_LIST -> {
                controller.looper?.setImageResource(R.drawable.ic_round_repeat_24_white)
                showToast(controller.context.getString(R.string.loop_list))
            }
            LOOP_SINGLE -> {
                controller.looper?.setImageResource(R.drawable.ic_round_repeat_one_24_white)
                showToast(controller.context.getString(R.string.loop_single))
            }
            PLAY_LIST -> {
                controller.looper?.setImageResource(R.drawable.ic_round_playlist_play_24_white)
                showToast(controller.context.getString(R.string.play_list))
            }
            NO_LOOP -> {
                controller.looper?.setImageResource(R.drawable.ic_round_block_24_white)
                showToast(controller.context.getString(R.string.no_loop))
            }
            RANDOM -> {
                controller.looper?.setImageResource(R.drawable.ic_round_loop_24_white)
                showToast(controller.context.getString(R.string.random))
            }
        }
    }

    fun refresh(music: Music, progress: Long) = binder?.init(music, progress)

    fun play(music: Music?) = binder?.play(music)

    fun setMusicList(mutableList: MutableList<Music>) = binder?.setPlayList(mutableList)

    fun getPlayingMusic() = binder?.onMusicPlaying()?.value

    fun finish() = controller.cancel()

    private fun showToast(msg: String) {
        Toast.makeText(controller.context, msg, Toast.LENGTH_SHORT).show()
    }
}