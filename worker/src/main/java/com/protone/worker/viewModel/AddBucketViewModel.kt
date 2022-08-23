package com.protone.worker.viewModel

import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.entity.MusicBucket
import com.protone.api.todayDate
import com.protone.worker.Medias
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper

class AddBucketViewModel : BaseViewModel() {

    companion object {
        @JvmStatic
        val BUCKET_NAME = "BUCKET_NAME"
    }

    sealed class AddBucketEvent {
        object Confirm : ViewEvent
        object ChooseIcon : ViewEvent
        object Cancel : ViewEvent
    }

    var editName: String? = null
    var musicBucket: MusicBucket? = null

    fun filterMusicBucket(name: String) {
        if (Medias.musicBucket.containsKey(editName)) {
            Medias.musicBucket[name] =
                Medias.musicBucket.remove(editName)
                    ?: mutableListOf()
        }
    }

    fun addMusicBucket(
        name: String,
        uri: Uri?,
        detail: String,
        callback: (Boolean, String) -> Unit
    ) {
        DatabaseHelper.instance
            .musicBucketDAOBridge.addMusicBucketWithCallBack(
                MusicBucket(
                    name,
                    uri?.saveToFile(name, R.string.music_bucket.getString()),
                    0,
                    detail,
                    todayDate("yyyy/MM/dd")
                )
            ) { result, rName ->
                callback.invoke(result, rName)
            }
    }

    suspend fun updateMusicBucket(
        musicBucket: MusicBucket, name: String, uri: Uri?, detail: String
    ) = DatabaseHelper.instance.musicBucketDAOBridge.updateMusicBucketRs(
        musicBucket.also { mb ->
            if (mb.name != name) mb.name = name
            val toFile = uri?.saveToFile(name, R.string.music_bucket.getString())
            if (mb.icon?.equals(toFile) == false) mb.icon = toFile
            if (mb.detail != detail) mb.detail = detail
            todayDate("yyyy/MM/dd")
        }
    )

    suspend fun getMusicBucketByName(name: String) =
        DatabaseHelper.instance.musicBucketDAOBridge.getMusicBucketByNameRs(name)

}