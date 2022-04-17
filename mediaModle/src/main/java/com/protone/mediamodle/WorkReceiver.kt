package com.protone.mediamodle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.TAG
import com.protone.api.context.*

val workLocalBroadCast by lazy { LocalBroadcastManager.getInstance(Global.application) }

abstract class WorkReceiver : BroadcastReceiver() ,IWorkService {

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(TAG, "onReceive: ")
        when (p1?.action) {
            UPDATE_MUSIC_BUCKET -> updateMusicBucket()
            UPDATE_MUSIC -> updateMusic()
            UPDATE_PHOTO -> updatePhoto()
            UPDATE_VIDEO -> updateVideo()
        }
    }
}