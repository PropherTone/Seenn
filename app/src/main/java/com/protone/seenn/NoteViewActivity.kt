package com.protone.seenn

import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.seen.NoteViewSeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class NoteViewActivity : BaseActivity<NoteViewSeen>() {

    companion object {
        const val NOTE_NAME = "NOTE_NAME"
    }

    override suspend fun main() {
        val noteViewSeen = NoteViewSeen(this)

        noteViewSeen.initSeen()

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                noteViewSeen.viewEvent.onReceive {
                    when (it) {
                        NoteViewSeen.NoteViewEvent.Finish -> finish()
                    }
                }
            }
        }
    }

    private suspend fun NoteViewSeen.initSeen() {
        intent.getStringExtra(NOTE_NAME)?.let { name ->
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<Note?> { co ->
                    co.resumeWith(Result.success(DataBaseDAOHelper.getNoteByName(name)))
                }
            }.let { note ->
                if (note != null) {
                    initNote(note)
                } else toast(getString(R.string.come_up_unknown_error))
            }
        }
    }
}