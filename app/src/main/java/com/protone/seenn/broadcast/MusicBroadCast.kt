package com.protone.seenn.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.context.*

const val LOOPING = 1
const val LIST_LOOPING = 2
const val RANDOM = 3

val musicBroadCastManager by lazy { LocalBroadcastManager.getInstance(Global.application) }

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
            }
            MUSIC_FINISH -> {
                finish()
                isPlaying = false
            }
            MUSIC_PREVIOUS -> {
                previous()
                isPlaying = true
            }
            MUSIC_NEXT -> {
                next()
                isPlaying = true
            }
        }
    }

    abstract fun play()
    abstract fun pause()
    abstract fun finish()
    abstract fun previous()
    abstract fun next()
}