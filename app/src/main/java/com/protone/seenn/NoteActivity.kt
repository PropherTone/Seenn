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

    private var selected: String? = null

    override suspend fun main() {

        val noteSeen = NoteSeen(this)

        noteSeen.initSeen()

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnResume -> {
                            queryAllNote().let { list ->
                                noteSeen.refreshNoteList(list)
                            }
                        }
                        else -> {}
                    }
                }
                noteSeen.viewEvent.onReceive {
                    when (it) {
                        NoteSeen.NoteEvent.Finish -> finish()
                    }
                }
            }
        }
    }

    private fun NoteSeen.initSeen() {
        initList()
        addNoteType {
            startActivity(NoteEditActivity::class.intent.also { intent ->
                intent.putExtra(NoteEditActivity.NOTE_TYPE, it)
            })
        }
        onTypeSelected { type ->
            refreshNoteList(noteList[type.also { selected = it }] ?: mutableListOf())
        }
        setNoteClk { s ->
            startActivity(NoteViewActivity::class.intent.also {
                it.putExtra(NoteViewActivity.NOTE_NAME, s)
            })
        }
    }

    private suspend fun queryAllNote() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<Note>> { co ->
            DataBaseDAOHelper.getAllNote()?.let {
                noteList[getString(R.string.all).also { s ->
                    selected = s
                }] = mutableListOf<Note>().apply { addAll(it) }
                it.forEach { note ->
                    if (noteList[note.type] == null) {
                        noteList[note.type] = mutableListOf()
                    }
                    if (note.type != getString(R.string.all)) noteList[note.type]?.add(note)
                }
                co.resumeWith(Result.success(noteList[selected] ?: mutableListOf()))
            }
        }
    }

}