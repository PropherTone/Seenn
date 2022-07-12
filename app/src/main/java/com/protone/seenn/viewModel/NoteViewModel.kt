package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protone.api.context.SApplication
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteType
import com.protone.seen.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class NoteViewModel : ViewModel() {

    enum class ViewEvent {
        Init,
        RefreshList
    }

    private val noteList = mutableMapOf<String, MutableList<Note>>()

    var selected: String? = null

    fun getNoteList(type: String?) = noteList[type.also { selected = it }] ?: mutableListOf()

    fun deleteNote(note: Note) = DataBaseDAOHelper.run {
        deleteNote(note)
        viewModelScope.launch(Dispatchers.IO) {
            getAllSignedMedia()?.forEach {
                it.notes?.let { notes ->
                    val index = notes.indexOf(note.title)
                    if (index != -1) {
                        notes as MutableList<String>
                        notes.removeAt(index)
                        updateSignedMedia(it)
                    }
                }
            }
        }
    }

    suspend fun queryAllNote() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<Note>> { co ->
            DataBaseDAOHelper.getAllNote()?.let {
                noteList[SApplication.app.getString(R.string.all).also { s ->
                    selected = s
                }] = mutableListOf<Note>().apply { addAll(it) }
                it.forEach { note ->
                    note.type.forEach { type ->
                        if (noteList[type] == null) {
                            noteList[type] = mutableListOf()
                        }
                        if (type != SApplication.app.getString(R.string.all) && noteList[type]?.contains(
                                note
                            ) == false
                        ) noteList[type]?.add(note)
                    }
                }
                co.resumeWith(Result.success(noteList[selected] ?: mutableListOf()))
            }
        }
    }

    suspend fun queryAllNoteType() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<NoteType>> { co ->
            co.resumeWith(
                Result.success(
                    (DataBaseDAOHelper.getALLNoteType() ?: mutableListOf()) as MutableList<NoteType>
                )
            )
        }
    }

    suspend fun insertNoteType(type: String?, image: String?) =
        DataBaseDAOHelper.insertNoteTypeRs(NoteType(type, image))
}