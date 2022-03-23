package com.protone.cloud.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.Nullable
import com.protone.cloud.noteSync.CloudStates
import com.protone.cloud.noteSync.NoteSyncClient
import com.protone.cloud.noteSync.NoteSyncServer
import com.protone.database.room.entity.Note

class NoteSyncService : Service() {

    private var noteSyncServer: NoteSyncServer? = null
    private var noteSyncClient: NoteSyncClient? = null

    override fun onBind(p0: Intent?): IBinder {
        return SyncBinder()
    }

    inner class SyncBinder : Binder() {

        fun connect(
            hostName: String? = null,
            port: Int,
            @Nullable statesString: CloudStates<String>? = null,
            @Nullable statesNote: CloudStates<Note>? = null
        ) {
            this@NoteSyncService.iConnect(
                hostName,
                port,
                statesString,
                statesNote
            )
        }

        fun startFrequency(data: List<Note>?) {
            this@NoteSyncService.iStartFrequency(data)
        }

    }

    private fun iStartFrequency(data: List<Note>?) {
        if (data == null) {
            noteSyncClient?.startReceiveFrequency()
        } else {
            noteSyncServer?.startSendFrequency(data)
        }
    }

    private fun iConnect(
        hostName: String?,
        port: Int,
        statesString: CloudStates<String>? = null,
        statesNote: CloudStates<Note>? = null
    ) {
        if (hostName == null) {
            noteSyncServer = NoteSyncServer().apply {
                connect(port,statesString)
            }
        } else {
            noteSyncClient = NoteSyncClient().apply {
                connect(hostName, port, statesNote)
            }
        }
    }

    private interface SyncInet {
        fun connect(
            hostName: String?,
            port: Int,
            statesString: CloudStates<String>? = null,
            statesNote: CloudStates<Note>? = null
        )

        fun startFrequency(data: List<Note>?)
    }
}