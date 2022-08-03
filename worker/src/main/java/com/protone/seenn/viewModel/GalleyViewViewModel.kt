package com.protone.seenn.viewModel

import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
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

    sealed class GalleyViewEvent {
        object SetNote : ViewEvent
    }

    var curPosition: Int = 0
    lateinit var galleyMedias: MutableList<GalleyMedia>

    suspend fun initGalleyData(galley: String, isVideo: Boolean) = withContext(Dispatchers.IO) {
        var allMedia = (DatabaseHelper.instance.signedGalleyDAOBridge.getAllMediaByType(isVideo)
            ?: mutableListOf()) as MutableList<GalleyMedia>
        if (galley != R.string.all_galley.getString()) allMedia =
            allMedia.stream().filter {
                (it.bucket == galley) || (it.type?.contains(galley) == true)
            }.collect(Collectors.toList())
        galleyMedias = allMedia
    }

    suspend fun getSignedMedia() =
        DatabaseHelper.instance.signedGalleyDAOBridge.getSignedMediaRs(galleyMedias[curPosition].uri)

    fun getCurrentMedia() = galleyMedias[curPosition]

    suspend fun getMediaByUri(uri: Uri) =
        DatabaseHelper.instance.signedGalleyDAOBridge.getSignedMediaRs(uri)

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


}