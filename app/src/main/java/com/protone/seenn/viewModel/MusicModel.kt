package com.protone.seenn.viewModel

import android.animation.ObjectAnimator
import androidx.lifecycle.ViewModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.baseType.toast
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.seenn.R
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
        val list = DataBaseDAOHelper.getAllMusicBucketRs()
        return suspendCoroutine { co ->
            if (list == null || list.isEmpty()) {
                DataBaseDAOHelper.addMusicBucketWithCallBack(
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
                    if (re) co.resumeWith(Result.success(DataBaseDAOHelper.getAllMusicBucketRs() as MutableList<MusicBucket>))
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
        DataBaseDAOHelper.getMusicBucketByName(bucket)
    }

    suspend fun doDeleteBucket(musicBucket: MusicBucket) =
        DataBaseDAOHelper.deleteMusicBucketRs(musicBucket)

    suspend fun deleteMusicBucketCover(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isFile && file.exists()) {
            file.delete()
        }
    }

    fun getMusicBucketByName(name: String) = DataBaseDAOHelper.getMusicBucketByName(name)

}