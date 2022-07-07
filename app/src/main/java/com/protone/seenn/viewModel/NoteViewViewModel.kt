package com.protone.seenn.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import java.util.stream.Collectors

class NoteViewViewModel : ViewModel() {

    enum class ViewEvent{
        Next,
        Edit
    }

    companion object {
        const val NOTE_NAME = "NOTE_NAME"
    }

    val noteQueue = ArrayDeque<String>()

    suspend fun getNoteByName(name: String) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Note?> { co ->
            co.resumeWith(Result.success(DataBaseDAOHelper.getNoteByName(name)))
        }
    }

    suspend fun getMusicByUri(uri: Uri) = withContext(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicByUri(uri)
    }

    suspend fun filterMedia(uri: Uri, isVideo: Boolean): MutableList<GalleyMedia>? =
        withContext(Dispatchers.IO) {
            DataBaseDAOHelper.getAllMediaByType(isVideo)?.stream()
                ?.filter { media -> media.uri == uri }
                ?.collect(Collectors.toList())
        }

}