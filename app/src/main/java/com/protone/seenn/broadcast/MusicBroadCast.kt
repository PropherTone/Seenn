package com.protone.seenn.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.context.*

val musicBroadCastManager by lazy { LocalBroadcastManager.getInstance(Global.app) }

abstract class MusicReceiver : BroadcastReceiver() {

    private var isPlaying = false

    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.action) {
            MUSIC_PLAY -> {
                isPlaying = if (!isPlaying) {
                    play()
                    true
                } else {
                    pause()
                    false
                }
                refresh(isPlaying)
            }
            MUSIC_FINISH -> {
                finish()
                isPlaying = false
            }
            MUSIC_PREVIOUS -> {
                previous()
                isPlaying = true
                refresh(isPlaying)
            }
            MUSIC_NEXT -> {
                next()
                isPlaying = true
                refresh(isPlaying)
            }
            MUSIC_REFRESH ->{
                refresh(isPlaying, ref = true)
            }
            MUSIC_PLAY_CUR->{
                refresh(true)
            }
        }
    }

    abstract fun play()
    abstract fun pause()
    abstract fun finish()
    abstract fun previous()
    abstract fun next()
    abstract fun refresh(b:Boolean,ref:Boolean = false)
}