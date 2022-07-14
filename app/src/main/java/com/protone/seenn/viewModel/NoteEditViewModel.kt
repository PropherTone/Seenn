package com.protone.seenn.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.toMediaBitmapByteArray
import com.protone.api.baseType.toast
import com.protone.api.json.toUriJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleriesWithNotes
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteDirWithNotes
import com.protone.mediamodle.GalleyHelper
import com.protone.seenn.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class NoteEditViewModel : ViewModel() {

    enum class ViewEvent {
        Confirm,
        PickImage,
        PickVideo,
        PickMusic,
        PickIcon
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

    suspend fun getAllNote() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<MutableList<String>> {
            val notes = DataBaseDAOHelper.getAllNote()
            val list = mutableListOf<String>()
            notes?.forEach { note ->
                list.add(note.title)
            }
            it.resumeWith(Result.success(list))
        }
    }

    suspend fun getMusicTitle(uri: Uri) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<String> { co ->
            val musicByUri = DataBaseDAOHelper.getMusicByUri(uri)
            co.resumeWith(
                Result.success(musicByUri?.title ?: "^ ^")
            )
        }
    }

    suspend fun copyNote(inNote: Note, note: Note) = withContext(Dispatchers.IO) {
        inNote.title = note.title
        inNote.text = note.text
        inNote.imagePath = note.imagePath
        inNote.richCode = note.richCode
        inNote.time = note.time
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        DataBaseDAOHelper.updateNote(note)
    }

    suspend fun insertNote(note: Note, dir: String?) = withContext(Dispatchers.IO) {
        DataBaseDAOHelper.insertNoteRs(note).let { result ->
            if (result.first) {
                DataBaseDAOHelper.deleteNote(note)
                dir?.let {
                    val noteDir = DataBaseDAOHelper.getNoteDir(it)
                    if (noteDir != null) {
                        DataBaseDAOHelper.insertNoteDirWithNote(
                            NoteDirWithNotes(
                                noteDir.noteDirId,
                                result.second
                            )
                        )
                    }
                }
                medias.forEach {
                    if (it.mediaId != null) {
                        DataBaseDAOHelper.insertGalleriesWithNotes(
                            GalleriesWithNotes(
                                it.mediaId!!,
                                result.second
                            )
                        )
                    }
                }
                true
            } else {
                R.string.failed_msg.getString().toast()
                false
            }
        }
    }

    suspend fun getNoteByName(name: String) = withContext(Dispatchers.IO) {
        DataBaseDAOHelper.getNoteByName(name)
    }
}