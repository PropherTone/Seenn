package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.seenn.R

class SplashViewModel : ViewModel() {

    enum class ViewEvent{
        InitConfig,
        UpdateMedia
    }

    fun firstBootWork(){
        if (userConfig.isFirstBoot) {
            DataBaseDAOHelper.addMusicBucketThread(
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
            )
            DataBaseDAOHelper.insertMusicMulti(Medias.music)
            userConfig.apply {
                isFirstBoot = false
                lastMusicBucket = R.string.all_music.getString()
                playedMusicPosition = -1
            }
        }
    }
}