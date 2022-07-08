package com.protone.seenn.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Medias
import com.protone.seenn.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddBucketViewModel : ViewModel() {

    companion object {
        @JvmStatic
        val BUCKET_NAME = "BUCKET_NAME"
    }

    enum class ViewEvent {
        Confirm
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
        DataBaseDAOHelper.addMusicBucketWithCallBack(
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
    ) = DataBaseDAOHelper.updateMusicBucketRs(
        musicBucket.also { mb ->
            if (mb.name != name) mb.name = name
            val toFile = uri?.saveToFile(name, R.string.music_bucket.getString())
            if (mb.icon?.equals(toFile) == false) mb.icon = toFile
            if (mb.detail != detail) mb.detail = detail
            todayDate("yyyy/MM/dd")
        }
    )

    suspend fun getMusicBucketByName(name: String) =
        withContext(Dispatchers.IO) { DataBaseDAOHelper.getMusicBucketByName(name) }
}