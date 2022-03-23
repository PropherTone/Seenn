package com.protone.seenn

import android.content.Intent
import android.util.Log
import android.widget.PopupWindow
import com.protone.seen.NoteSyncSeen
import com.protone.seenn.viewModel.NoteSyncModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import com.protone.api.TAG
import com.protone.api.context.intent
import com.protone.cloud.service.NoteSyncService

class NoteSyncActivity : BaseActivity<NoteSyncSeen>() {

    private val model: NoteSyncModel by viewModels()

    override suspend fun main() {
        val noteSyncSeen = NoteSyncSeen(this)
        setContentSeen(noteSyncSeen)

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

}