package com.protone.seenn.service

import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import com.protone.api.context.MUSIC_NEXT
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.MUSIC_PREVIOUS
import com.protone.seen.customView.ColorfulProgressBar
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer
import com.protone.seenn.broadcast.MusicSer
import com.protone.seenn.broadcast.musicBroadCastManager

class MusicControllerIMP(
    lifecycle: LifecycleOwner,
    controller: BaseMusicPlayer,
    binder: MusicSer.MusicBinder
) {
    init {
        controller.apply {
            control.setOnClickListener {
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_PLAY))
            }
            next.setOnClickListener {
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_NEXT))
            }
            previous.setOnClickListener {
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_PREVIOUS))
            }
            binder.run {
                progress.progressListener = object : ColorfulProgressBar.Progress {
                    override fun getProgress(position: Long) {
                        setProgress(position)
                    }
                }
                onProgress().observe(lifecycle) {
                    progress.barSeekTo(it)
                }
                onPlayState().observe(lifecycle) {
                    if (it) onPlay() else onPause()
                }
            }
        }
    }
}