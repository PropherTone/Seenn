package com.protone.seenn.viewModel

import android.animation.ObjectAnimator
import androidx.lifecycle.ViewModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.baseType.toast
import com.protone.api.entity.MusicBucket
import com.protone.api.todayDate
import com.protone.seenn.Medias
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import com.protone.seenn.database.userConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.stream.Collectors
import kotlin.coroutines.suspendCoroutine

class MusicModel : ViewModel() {

    enum class ViewEvent {
        Delete,
        RefreshBucket
    }

    var bucket = R.string.all_music.getString()
    var lastBucket = userConfig.lastMusicBucket
    var containerAnimator: ObjectAnimator? = null
    var actionPosition: Int = 0

    fun getMusicList() = Medias.musicBucket[bucket] ?: mutableListOf()

    suspend fun filterBucket(): MusicBucket =
        getMusicBucket().stream().filter { it.name == lastBucket }
            .collect(Collectors.toList()).let {
                if (it.size > 0) it[0] else MusicBucket()
            }

    suspend fun getMusicBucket(): MutableList<MusicBucket> {
        val list = DatabaseHelper.instance.musicBucketDAOBridge.getAllMusicBucketRs()
        return suspendCoroutine { co ->
            if (list == null || list.isEmpty()) {
                DatabaseHelper.instance.musicBucketDAOBridge.addMusicBucketWithCallBack(
                    MusicBucket(
                        R.string.all_music.getString(),
                        if (Medias.music.size > 0) Medias.music[0].uri.saveToFile(
                            R.string.all_music.getString(),
                            R.string.music_bucket.getString()
                        ) else null,
                        Medias.music.size,
                        null,
                        todayDate("yyyy/MM/dd")
                    )
                ) { re, _ ->
                    if (re) co.resumeWith(
                        Result.success(
                            DatabaseHelper
                                .instance
                                .musicBucketDAOBridge
                                .getAllMusicBucketRs() as MutableList<MusicBucket>
                        )
                    )
                }
            } else co.resumeWith(Result.success(list as MutableList<MusicBucket>))
        }
    }

    fun compareName(): Boolean {
        if (bucket == "" || bucket == R.string.all_music.getString()) {
            R.string.none.getString().toast()
            return true
        }
        return false
    }

    suspend fun getBucket() = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.musicBucketDAOBridge.getMusicBucketByName(bucket)
    }

    suspend fun doDeleteBucket(musicBucket: MusicBucket) =
        DatabaseHelper.instance.musicBucketDAOBridge.deleteMusicBucketRs(musicBucket)

    suspend fun deleteMusicBucketCover(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isFile && file.exists()) {
            file.delete()
        }
    }

    fun getMusicBucketByName(name: String) =
        DatabaseHelper.instance.musicBucketDAOBridge.getMusicBucketByName(name)

}