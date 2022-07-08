package com.protone.seenn.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.context.SApplication
import com.protone.api.context.UPDATE_GALLEY
import com.protone.api.context.UPDATE_MUSIC
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.api.json.toUri

val workLocalBroadCast by lazy { LocalBroadcastManager.getInstance(SApplication.app) }

abstract class WorkReceiver : BroadcastReceiver(), IWorkService {

    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.action) {
            UPDATE_MUSIC_BUCKET -> updateMusicBucket()
            UPDATE_MUSIC -> updateMusic(p1.getStringExtra("uri")?.toUri())
            UPDATE_GALLEY -> updateGalley(p1.getStringExtra("uri")?.toUri())
        }
    }
}

interface IWorkService {
    fun updateMusicBucket()
    fun updateMusic(data: Uri?)
    fun updateGalley(data: Uri?)
}