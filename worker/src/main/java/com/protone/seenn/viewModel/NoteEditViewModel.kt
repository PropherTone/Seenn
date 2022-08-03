package com.protone.seenn.viewModel

import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.baseType.toMediaBitmapByteArray
import com.protone.api.baseType.toast
import com.protone.api.entity.GalleriesWithNotes
import com.protone.api.entity.GalleyMedia
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDirWithNotes
import com.protone.api.json.toUriJson
import com.protone.api.onResult
import com.protone.seenn.GalleyHelper
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteEditViewModel : BaseViewModel() {

    sealed class NoteEvent {
        object Confirm : ViewEvent
        object PickImage : ViewEvent
        object PickVideo : ViewEvent
        object PickMusic : ViewEvent
        object PickIcon : ViewEvent
    }

    companion object {
        const val NOTE_DIR = "NoteType"
        const val NOTE = "Note"
        const val CONTENT_TITLE = "NoteContentTitle"
    }

    var savedIconPath: String = ""
    var iconUri: Uri? = null
    var noteByName: Note? = null
    var noteName: String? = null
    var allNote: MutableList<String>? = null
    var onEdit = false
    var medias = arrayListOf<GalleyMedia>()

    suspend fun saveIcon(name: String, onResult: (Boolean) -> Unit): Unit =
        withContext(Dispatchers.IO) {
            GalleyHelper.saveIconToLocal(
                name,
                iconUri?.toMediaBitmapByteArray()
            ) { s ->
                savedIconPath = if (!s.isNullOrEmpty()) {
                    onResult.invoke(true)
                    s
                } else {
                    R.string.failed_upload_image.getString().toast()
                    onResult.invoke(false)
                    iconUri!!.toUriJson()
                }
            }
        }

    suspend fun getAllNote() = onResult {
        val notes = DatabaseHelper.instance.noteDAOBridge.getAllNote()
        val list = mutableListOf<String>()
        notes?.forEach { note ->
            list.add(note.title)
        }
        it.resumeWith(Result.success(list))
    }

    suspend fun getMusicTitle(uri: Uri) = onResult { co ->
        val musicByUri = DatabaseHelper.instance.musicDAOBridge.getMusicByUri(uri)
        co.resumeWith(
            Result.success(musicByUri?.title ?: "^ ^")
        )
    }

    suspend fun copyNote(inNote: Note, note: Note) = withContext(Dispatchers.IO) {
        inNote.title = note.title
        inNote.text = note.text
        inNote.imagePath = note.imagePath
        inNote.richCode = note.richCode
        inNote.time = note.time
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.noteDAOBridge.updateNote(note)
    }

    suspend fun insertNote(note: Note, dir: String?) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.noteDAOBridge.insertNoteRs(note).let { result ->
            if (result.first) {
                DatabaseHelper.instance.noteDAOBridge.deleteNote(note)
                dir?.let {
                    val noteDir = DatabaseHelper.instance.noteDirDAOBridge.getNoteDir(it)
                    if (noteDir != null) {
                        DatabaseHelper
                            .instance
                            .noteDirWithNoteDAOBridge
                            .insertNoteDirWithNote(
                                NoteDirWithNotes(
                                    noteDir.noteDirId,
                                    result.second
                                )
                            )
                    }
                }
                medias.forEach {
                    DatabaseHelper
                        .instance
                        .galleriesWithNotesDAOBridge
                        .insertGalleriesWithNotes(GalleriesWithNotes(it.uri, result.second))
                }
                true
            } else {
                R.string.failed_msg.getString().toast()
                false
            }
        }
    }

    suspend fun getNoteByName(name: String) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.noteDAOBridge.getNoteByName(name)
    }
}