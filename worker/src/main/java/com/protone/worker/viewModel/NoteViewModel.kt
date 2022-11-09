package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.baseType.*
import com.protone.api.context.SApplication
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.api.entity.NoteDirWithNotes
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.MediaAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect

class NoteViewModel : BaseViewModel() {

    sealed class NoteViewEvent : ViewEvent {
        object RefreshList : NoteViewEvent()
        object AddBucket : NoteViewEvent()
        object Refresh : NoteViewEvent()
        object HandleBucketEvent : NoteViewEvent()
    }

    private val noteList = mutableMapOf<String, MutableList<Note>>()

    private var selected: String? = null

    private var noteDirWatcherJob: Job? = null

    private var dirEmitter: ((List<NoteDir>) -> Unit)? = null
    private var notesEmitter: ((List<Note>) -> Unit)? = null

    private fun createNoteDirJob(dir: NoteDir) {
        noteDirWatcherJob?.cancel()
        noteDirWatcherJob = if (dir.name == R.string.all.getString()) {
            viewModelScope.launchDefault {
                DatabaseHelper.instance.noteDAOBridge.getAllNoteFlow().collect { notes ->
                    notes.let { nonNullNotes -> notesEmitter?.invoke(nonNullNotes) }
                }
            }
        } else viewModelScope.launchDefault {
            DatabaseHelper.instance.noteDirWithNoteDAOBridge
                .getNotesWithNoteDirFlow(dir.noteDirId)
                .collect { notes ->
                    notes?.let { nonNullNotes -> notesEmitter?.invoke(nonNullNotes) }
                }
        }.also { it.start() }
    }

    private suspend fun getNoteDir(note: Note): List<NoteDir> = DatabaseHelper.instance
        .noteDirWithNoteDAOBridge
        .getNoteDirWithNote(note.noteId)

    fun collectNoteEvent(callBack: suspend (MediaAction) -> Unit) {
        viewModelScope.launchDefault {
            DatabaseHelper.instance.mediaNotifier.bufferCollect {
                callBack.invoke(it)
            }
        }
    }

    fun watchNoteDirs(func: (List<NoteDir>) -> Unit) {
        this.dirEmitter = func
    }

    fun watchNotes(func: (List<Note>) -> Unit) {
        this.notesEmitter = func
    }

    suspend fun getNoteList(type: NoteDir) =
        DatabaseHelper
            .instance
            .let {
                if (type.name == R.string.all.getString()) {
                    createNoteDirJob(type)
                    it.noteDAOBridge.getAllNote() ?: listOf()
                } else it.noteDirWithNoteDAOBridge
                    .getNotesWithNoteDir(type.noteDirId).also {
                        createNoteDirJob(type)
                    }
            }

    fun insertNewNoteToNoteDir(noteDirWithNotes: NoteDirWithNotes) {
        viewModelScope.launchIO {
            DatabaseHelper.instance
                .noteDAOBridge
                .getNoteById(noteDirWithNotes.noteId)?.let { note ->
                    noteList[R.string.all.getString()]?.add(note)
                    val noteDir = getNoteDir(note)
                    noteDir.forEach { noteList[it.name]?.add(note) }
                }
        }
    }

    fun removeNote(note: Note) {
        viewModelScope.launchDefault {
            noteList[R.string.all.getString()]?.remove(note)
            getNoteDir(note).forEach {
                noteList[it.name]?.remove(note)
            }
        }
    }

    fun addNote(note: Note) {
        viewModelScope.launchDefault {
            val all = R.string.all.getString()
            getNoteDir(note).forEach {
                noteList[all]?.add(note)
                noteList[it.name]?.add(note)
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launchDefault {
            val all = R.string.all.getString()
            getNoteDir(note).forEach {
                noteList[all]?.replaceAll { note ->
                    if (note.noteId == note.noteId) {
                        note
                    } else note
                }
                noteList[it.name]?.replaceAll { note ->
                    if (note.noteId == note.noteId) {
                        note
                    } else note
                }
            }
        }
    }

    fun deleteNoteCache(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            note.imagePath?.deleteFile()
            "${SApplication.app.filesDir.absolutePath}/${note.title}/".deleteFile()
        }
    }

    fun deleteNote(note: Note) {
        DatabaseHelper.instance.noteDAOBridge.deleteNoteAsync(note)
    }

    fun deleteNoteDir(noteType: NoteDir) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHelper.instance.noteDirDAOBridge.doDeleteNoteDirRs(noteType)
        }
    }

    suspend fun getNote(title: String) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.noteDAOBridge.getNoteByName(title)
    }

    suspend fun queryAllNote() = withContext(Dispatchers.IO) {
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
            noteList[selected]
        } ?: mutableListOf()
    }

    suspend fun queryAllNoteType() = withContext(Dispatchers.IO) {
        (DatabaseHelper
            .instance
            .noteDirDAOBridge
            .getALLNoteDir() ?: mutableListOf())
                as MutableList<NoteDir>
    }

    suspend fun insertNoteDir(type: String?, image: String?) =
        DatabaseHelper.instance
            .noteDirDAOBridge
            .insertNoteDirRs(NoteDir(type, image))

}