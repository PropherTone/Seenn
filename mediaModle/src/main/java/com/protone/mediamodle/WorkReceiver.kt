package com.protone.mediamodle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.context.*

val workLocalBroadCast by lazy { LocalBroadcastManager.getInstance(Global.application) }

abstract class WorkReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.action) {
            UPDATE_MUSIC_BUCKET -> updateMusicBucket()
            UPDATE_MUSIC -> updateMusic()
            UPDATE_PHOTO -> updatePhoto()
            UPDATE_VIDEO -> updateVideo()
        }
    }

    abstract fun updateMusicBucket()
    abstract fun updateMusic()
    abstract fun updatePhoto()
    abstract fun updateVideo()
}