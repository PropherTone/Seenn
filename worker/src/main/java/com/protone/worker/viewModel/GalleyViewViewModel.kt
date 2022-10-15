package com.protone.worker.viewModel

import android.net.Uri
import com.protone.api.baseType.deleteFile
import com.protone.api.baseType.getString
import com.protone.api.baseType.imageSaveToDisk
import com.protone.api.entity.GalleryMedia
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors
import kotlin.streams.toList

class GalleryViewViewModel : BaseViewModel() {

    companion object {
        const val MEDIA = "GalleryViewActivity:MediaData"
        const val IS_VIDEO = "GalleryViewActivity:IsVideo"
        const val GALLERY = "GalleryViewActivity:Gallery"
    }

    sealed class GalleryViewEvent : ViewEvent {
        object SetNote : GalleryViewEvent()
        object Share : GalleryViewEvent()
    }

    var curPosition: Int = 0
    lateinit var galleryMedias: MutableList<GalleryMedia>

    suspend fun initGalleryData(gallery: String, isVideo: Boolean) = withContext(Dispatchers.IO) {
        var allMedia = (DatabaseHelper.instance.signedGalleryDAOBridge.let {
            if (userConfig.combineGallery) it.getAllSignedMedia() else it.getAllMediaByType(isVideo)
        } ?: mutableListOf()) as MutableList<GalleryMedia>
        if (gallery != R.string.all_gallery.getString()) allMedia =
            allMedia.stream().filter {
                (it.bucket == gallery) || (it.type?.contains(gallery) == true)
            }.collect(Collectors.toList())
        galleryMedias = allMedia
    }

    suspend fun getSignedMedia() = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.signedGalleryDAOBridge.getSignedMedia(galleryMedias[curPosition].uri)
    }

    suspend fun getNotesWithGallery(mediaUri: Uri): MutableList<String> =
        withContext(Dispatchers.IO) {
            DatabaseHelper
                .instance
                .galleriesWithNotesDAOBridge
                .getNotesWithGallery(mediaUri)
                .stream()
                .map { note ->
                    note.title
                }.toList() as MutableList<String>
        }

    suspend fun prepareSharedMedia() = withContext(Dispatchers.IO) {
        getCurrentMedia().let {
            it.uri.imageSaveToDisk(it.name, "SharedMedia")
        }
    }

    suspend fun deleteSharedMedia(path: String) = withContext(Dispatchers.IO) {
        path.deleteFile()
    }

    fun getCurrentMedia() = galleryMedias[curPosition]


}