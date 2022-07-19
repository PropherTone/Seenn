package com.protone.seenn.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.database.DatabaseHelper
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
        suspendCancellableCoroutine { co ->
            co.resumeWith(Result.success(DatabaseHelper.instance.noteDAOBridge.getNoteByName(name)))
        }
    }

    suspend fun getMusicByUri(uri: Uri) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.musicDAOBridge.getMusicByUri(uri)
    }

    suspend fun filterMedia(uri: Uri, isVideo: Boolean): MutableList<GalleyMedia>? =
        withContext(Dispatchers.IO) {
            DatabaseHelper.instance.signedGalleyDAOBridge.getAllMediaByType(isVideo)?.stream()
                ?.filter { media -> media.uri == uri }
                ?.collect(Collectors.toList())
        }

}