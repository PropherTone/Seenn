package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.protone.api.baseType.getString
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteDir
import com.protone.seen.R
import kotlinx.coroutines.Dispatchers
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

    fun deleteNote(note: Note) = DataBaseDAOHelper.deleteNote(note)


    suspend fun queryAllNote() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<Note>> { co ->
            DataBaseDAOHelper.getALLNoteDir()?.let {
                noteList[R.string.all.getString().also { s ->
                    selected = s
                }] = mutableListOf<Note>().apply {
                    addAll(DataBaseDAOHelper.getAllNote() ?: mutableListOf())
                }
                it.forEach { noteDir ->
                    noteList[noteDir.name] =
                        DataBaseDAOHelper.getNotesWithNoteDir(noteDir.noteDirId) as MutableList<Note>
                }
                co.resumeWith(Result.success(noteList[selected] ?: mutableListOf()))
            }
        }
    }

    suspend fun queryAllNoteType() = withContext(Dispatchers.IO) {
        suspendCoroutine<MutableList<NoteDir>> { co ->
            co.resumeWith(
                Result.success(
                    (DataBaseDAOHelper.getALLNoteDir() ?: mutableListOf()) as MutableList<NoteDir>
                )
            )
        }
    }

    suspend fun insertNoteDir(type: String?, image: String?) =
        DataBaseDAOHelper.insertNoteDirRs(
            NoteDir(
                type,
                image
            )
        )
}