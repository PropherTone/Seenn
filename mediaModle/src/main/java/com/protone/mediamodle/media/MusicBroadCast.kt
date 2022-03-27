package com.protone.mediamodle.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.TAG
import com.protone.api.context.*

const val LOOPING = 1
const val LIST_LOOPING = 2
const val RANDOM = 3

val musicBroadCastManager by lazy { LocalBroadcastManager.getInstance(Global.application) }

abstract class MusicReceiver : BroadcastReceiver() {

    private var isPlaying = false

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(TAG, "onReceive: ")
        when (p1?.action) {
            MUSIC_PLAY -> {
                if (!isPlaying) {
                    isPlaying = true
                    play()
                } else {
                    isPlaying = false
                    pause()
                }
            }
            MUSIC_FINISH -> {
                finish()
            }
            MUSIC_PREVIOUS -> {
                previous()
            }
            MUSIC_NEXT -> {
                next()
            }
        }
    }

    abstract fun play()
    abstract fun pause()
    abstract fun finish()
    abstract fun previous()
    abstract fun next()
}