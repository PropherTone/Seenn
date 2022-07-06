package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.protone.api.getString
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.seenn.R
import java.util.stream.Collectors

class GalleyViewViewModel : ViewModel() {

    companion object {
        const val MEDIA = "GalleyViewActivity:MediaData"
        const val TYPE = "GalleyViewActivity:IsVideo"
        const val GALLEY = "GalleyViewActivity:Galley"
    }

    enum class ViewEvent{
        SetNote
    }

    var curPosition: Int = 0
    lateinit var galleyMedias: MutableList<GalleyMedia>

    fun initGalleyData(galley: String, isVideo: Boolean) {
        var allMedia = (DataBaseDAOHelper.getAllMediaByType(isVideo)
            ?: mutableListOf()) as MutableList<GalleyMedia>
        if (galley != R.string.all_galley.getString()) allMedia =
            allMedia.stream().filter {
                (it.bucket == galley) || (it.type?.contains(galley) == true)
            }.collect(Collectors.toList())
        galleyMedias = allMedia
    }

    suspend fun getSignedMedia() = DataBaseDAOHelper.getSignedMediaRs(galleyMedias[curPosition].uri)

    fun getCurrentMedia() =  galleyMedias[curPosition]
}