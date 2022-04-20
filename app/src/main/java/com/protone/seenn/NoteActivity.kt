package com.protone.seenn

import com.protone.api.context.intent
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.seen.NoteSeen
import com.protone.seen.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class NoteActivity : BaseActivity<NoteSeen>() {

    private val noteList = mutableMapOf<String, MutableList<Note>>()

    override suspend fun main() {

        val noteSeen = NoteSeen(this)

        noteSeen.initSeen()

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                noteSeen.viewEvent.onReceive {

                }
            }
        }
    }

    private suspend fun NoteSeen.initSeen() {
        initList()
        refreshNoteList(queryAllNote())
        addNoteType {
            startActivity(NoteEditActivity::class.intent.also { intent ->
                intent.putExtra(NoteEditActivity.NOTE_TYPE, it)
            })
        }
        onTypeSelected {
            refreshNoteList(noteList[it] ?: mutableListOf())
        }
    }

    private suspend fun queryAllNote() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<Note>> { co ->
            DataBaseDAOHelper.getAllNote()?.let {
                noteList[getString(R.string.all)] = mutableListOf<Note>().apply { addAll(it) }
                it.forEach { note ->
                    if (noteList[note.type] == null) {
                        noteList[note.type] = mutableListOf()
                    }
                    if (note.type != getString(R.string.all)) noteList[note.type]?.add(note)
                }
                co.resumeWith(Result.success(noteList[getString(R.string.all)] ?: mutableListOf()))
            }
        }
    }

}