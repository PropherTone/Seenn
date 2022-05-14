package com.protone.seenn

import com.protone.api.context.intent
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteType
import com.protone.seen.NoteSeen
import com.protone.seen.R
import com.protone.seen.dialog.TitleDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class NoteActivity : BaseActivity<NoteSeen>() {

    private val noteList = mutableMapOf<String, MutableList<Note>>()

    private var selected: String? = null

    override suspend fun main() {

        var noteSeen = NoteSeen(this)
        setContentSeen(noteSeen)
        noteSeen.initSeen()

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnResume -> noteSeen.refreshList()
                        else -> {}
                    }
                }
                noteSeen.viewEvent.onReceive {
                    when (it) {
                        NoteSeen.NoteEvent.Finish -> finish()
                        NoteSeen.NoteEvent.AddBucket -> {
                            TitleDialog(this@NoteActivity, getString(R.string.add_dir), "") { re ->
                                if (re.isNotEmpty()) {
                                    DataBaseDAOHelper.insertNoteTypeCB(
                                        NoteType(re, "")
                                    ) { suc, name ->
                                        if (suc) {
                                            noteSeen.insertNoteType(NoteType(name, ""))
                                        } else {
                                            toast(getString(R.string.failed_msg))
                                        }
                                    }
                                } else {
                                    toast(getString(R.string.enter))
                                }
                            }
                        }
                        NoteSeen.NoteEvent.Refresh -> {
                            noteSeen = NoteSeen(this@NoteActivity)
                            setContentSeen(noteSeen)
                            noteSeen.initSeen()
                            noteSeen.refreshList()
                        }
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
                    note.type.forEach { type ->
                        if (noteList[type] == null) {
                            noteList[type] = mutableListOf()
                        }
                        if (type != getString(R.string.all)) noteList[type]?.add(note)
                    }
                }
                co.resumeWith(Result.success(noteList[selected] ?: mutableListOf()))
            }
        }
    }

    private suspend fun queryAllNoteType() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<NoteType>> { co ->
            co.resumeWith(
                Result.success(
                    (DataBaseDAOHelper.getALLNoteType() ?: mutableListOf()) as MutableList<NoteType>
                )
            )
        }
    }

    private suspend fun NoteSeen.refreshList() {
        refreshNoteList(queryAllNote())
        refreshNoteType(queryAllNoteType())
    }

}