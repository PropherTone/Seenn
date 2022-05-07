package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.activity.viewModels
import com.protone.api.json.toJson
import com.protone.cloud.service.NoteSyncService
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.AppData
import com.protone.seen.NoteSyncSeen
import com.protone.seenn.viewModel.NoteSyncModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class NoteSyncActivity : BaseActivity<NoteSyncSeen>() {

    private val model: NoteSyncModel by viewModels()

    override suspend fun main() {
        val noteSyncSeen = NoteSyncSeen(this)
        setContentSeen(noteSyncSeen)

        withContext(Dispatchers.IO) {
            val appdata = DataBaseDAOHelper.let {
                AppData(
                    it.getAllMusic()?.toJson() ?: "",
                    it.getALLGalleyBucket()?.toJson() ?: "",
                    it.getALLNoteType()?.toJson() ?: "",
                    it.getAllMusicBucket()?.toJson() ?: "",
                    it.getAllNote()?.toJson() ?: "",
                    it.getAllSignedMedia()?.toJson() ?: ""
                )
            }
            Log.d("TAG", "main: $appdata")
            appdata
        }

        bindService(
            Intent(this@NoteSyncActivity, NoteSyncService::class.java),
            model.conn,
            BIND_AUTO_CREATE
        )

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnDestroy -> {
                            doOnFinish {
                                model.syncBinder
                                unbindService(model.conn)
                            }
                        }
                        else -> {}
                    }
                }
                noteSyncSeen.viewEvent.onReceive {
                    when (it) {
                        NoteSyncSeen.NoteSync.Send -> {
//                            model.syncBinder?.connect()
                        }
                        NoteSyncSeen.NoteSync.Receive -> {
//                            model.syncBinder?.connect()
                        }
                    }
                }
            }
        }
    }

    private fun NoteSyncSeen.popUp() {
        //
    }

    //                            bindService(
//                                NoteSyncService::class.intent.apply {
//                                    action = "SERVER"
//                                },
//                                object : IServerConnection() {
//                                    override fun onServiceConnected(
//                                        p0: ComponentName?,
//                                        p1: IBinder?
//                                    ) {
//                                        super.onServiceConnected(p0, p1)
//                                        (p1 as NoteSyncService.SyncBinder).connect(
//                                            port = 6666,
//                                            statesString = object : CloudStates<String> {
//                                                override fun success() {
//                                                    Log.d(TAG, "success: ")
//                                                }
//
//                                                override fun failed(msg: String) {
//                                                    Log.d(TAG, "failed: ")
//                                                }
//
//                                                override fun successMsg(arg: String) {
//                                                    Log.d(TAG, "successMsg: ${arg.toJson()}")
//                                                }
//
//                                            })
//                                    }
//                                },
//                                BIND_AUTO_CREATE
//                            )

}