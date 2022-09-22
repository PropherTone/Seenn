package com.protone.worker.viewModel

import android.net.Uri
import com.protone.api.baseType.deleteFile
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.entity.GalleyMedia
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors
import kotlin.streams.toList

class GalleyViewViewModel : BaseViewModel() {

    companion object {
        const val MEDIA = "GalleyViewActivity:MediaData"
        const val TYPE = "GalleyViewActivity:IsVideo"
        const val GALLEY = "GalleyViewActivity:Galley"
    }

    sealed class GalleyViewEvent : ViewEvent {
        object SetNote : GalleyViewEvent()
        object Share : GalleyViewEvent()
    }

    var curPosition: Int = 0
    lateinit var galleyMedias: MutableList<GalleyMedia>

    suspend fun initGalleyData(galley: String, isVideo: Boolean) = withContext(Dispatchers.IO) {
        var allMedia = (DatabaseHelper.instance.signedGalleyDAOBridge.let {
            if (userConfig.combineGalley) it.getAllSignedMedia() else it.getAllMediaByType(isVideo)
        } ?: mutableListOf()) as MutableList<GalleyMedia>
        if (galley != R.string.all_galley.getString()) allMedia =
            allMedia.stream().filter {
                (it.bucket == galley) || (it.type?.contains(galley) == true)
            }.collect(Collectors.toList())
        galleyMedias = allMedia
    }

    suspend fun getSignedMedia() = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.signedGalleyDAOBridge.getSignedMedia(galleyMedias[curPosition].uri)
    }

    suspend fun getNotesWithGalley(mediaUri: Uri): MutableList<String> =
        withContext(Dispatchers.IO) {
            DatabaseHelper
                .instance
                .galleriesWithNotesDAOBridge
                .getNotesWithGalley(mediaUri)
                .stream()
                .map { note ->
                    note.title
                }.toList() as MutableList<String>
        }

    suspend fun prepareSharedMedia() = withContext(Dispatchers.IO) {
        getCurrentMedia().let {
            it.uri.saveToFile(it.name, "SharedMedia")
        }
    }

    suspend fun deleteSharedMedia(path: String) = withContext(Dispatchers.IO) {
        path.deleteFile()
    }

    fun getCurrentMedia() = galleyMedias[curPosition]


}