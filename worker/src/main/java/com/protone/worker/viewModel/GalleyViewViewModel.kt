package com.protone.worker.viewModel

import android.net.Uri
import com.protone.api.baseType.*
import com.protone.api.entity.GalleryMedia
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
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

    suspend fun initGalleryData(gallery: String, isVideo: Boolean) = withDefaultContext {
        var allMedia = (DatabaseHelper.instance.signedGalleryDAOBridge.let {
            if (userConfig.combineGallery) it.getAllSignedMedia() else it.getAllMediaByType(isVideo)
        } ?: mutableListOf()) as MutableList<GalleryMedia>
        if (gallery != R.string.all_gallery.getString()) allMedia =
            allMedia.stream().filter {
                (it.bucket == gallery) || (it.type?.contains(gallery) == true)
            }.collect(Collectors.toList())
        galleryMedias = allMedia
    }

    suspend fun getSignedMedia() =
        DatabaseHelper.instance.signedGalleryDAOBridge.getSignedMedia(galleryMedias[curPosition].uri)

    suspend fun getNotesWithGallery(mediaUri: Uri): MutableList<String> =
        withDefaultContext {
            DatabaseHelper
                .instance
                .galleriesWithNotesDAOBridge
                .getNotesWithGallery(mediaUri)
                .stream()
                .map { note ->
                    note.title
                }.toList() as MutableList<String>
        }

    suspend fun prepareSharedMedia() = withIOContext {
        getCurrentMedia().let {
            it.uri.imageSaveToDisk(it.name, "SharedMedia")
        }
    }

    suspend fun deleteSharedMedia(path: String) = withIOContext {
        path.deleteFile()
    }

    suspend fun getMediaByUri(uri: Uri) =
        DatabaseHelper.instance.signedGalleryDAOBridge.getSignedMedia(uri)


    fun getCurrentMedia() = galleryMedias[curPosition]


}