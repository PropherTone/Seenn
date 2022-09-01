package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.baseType.deleteFile
import com.protone.api.baseType.getString
import com.protone.api.context.SApplication
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.MediaAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class NoteViewModel : BaseViewModel() {

    sealed class NoteViewEvent {
        object RefreshList : ViewEvent
        object AddBucket : ViewEvent
        object Refresh : ViewEvent
        object HandleBucketEvent : ViewEvent
    }

    private val noteList = mutableMapOf<String, MutableList<Note>>()

    private var selected: String? = null

    init {
        DatabaseHelper.instance.noteDAOBridge.startEvent()
    }

    override fun onCleared() {
        super.onCleared()
        DatabaseHelper.instance.noteDAOBridge.stopEvent()
    }

    fun collectNoteEvent(callBack:suspend (MediaAction) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            DatabaseHelper.instance.mediaNotifier.buffer().collect {
                callBack.invoke(it)
            }
        }
    }

    fun getNoteList(type: String?) = noteList[type.also { selected = it }] ?: mutableListOf()

    fun deleteNoteCache(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            note.imagePath?.deleteFile()
            "${SApplication.app.filesDir.absolutePath}/${note.title}/".deleteFile()
        }
    }

    fun deleteNote(note: Note) {
        DatabaseHelper.instance.noteDAOBridge.deleteNoteAsync(note)
    }

    suspend fun getNote(title: String) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.noteDAOBridge.getNoteByName(title)
    }

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