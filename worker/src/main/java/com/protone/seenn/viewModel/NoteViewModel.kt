package com.protone.seenn.viewModel

import com.protone.api.baseType.getString
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class NoteViewModel : BaseViewModel() {

    sealed class NoteViewEvent {
        object Init : ViewEvent
        object RefreshList : ViewEvent
    }

    private val noteList = mutableMapOf<String, MutableList<Note>>()

    private var selected: String? = null

    fun getNoteList(type: String?) = noteList[type.also { selected = it }] ?: mutableListOf()

    fun deleteNote(note: Note) = DatabaseHelper.instance.noteDAOBridge.deleteNoteAsync(note)


    suspend fun queryAllNote() = withContext(Dispatchers.IO) {
        suspendCoroutine { co ->
            DatabaseHelper.instance.noteDirDAOBridge.getALLNoteDir()?.let {
                noteList[R.string.all.getString().also { s ->
                    selected = s
                }] = mutableListOf<Note>().apply {
                    addAll(DatabaseHelper.instance.noteDAOBridge.getAllNote() ?: mutableListOf())
                }
                it.forEach { noteDir ->
                    noteList[noteDir.name] =
                        DatabaseHelper
                            .instance
                            .noteDirWithNoteDAOBridge
                            .getNotesWithNoteDir(noteDir.noteDirId) as MutableList<Note>
                }
                co.resumeWith(Result.success(noteList[selected] ?: mutableListOf()))
            }
        }
    }

    suspend fun queryAllNoteType() = withContext(Dispatchers.IO) {
        suspendCoroutine { co ->
            co.resumeWith(
                Result.success(
                    (DatabaseHelper
                        .instance
                        .noteDirDAOBridge
                        .getALLNoteDir() ?: mutableListOf()) as MutableList<NoteDir>
                )
            )
        }
    }

    suspend fun insertNoteDir(type: String?, image: String?) =
        DatabaseHelper
            .instance
            .noteDirDAOBridge
            .insertNoteDirRs(NoteDir(type, image))
}