package com.protone.seenn.viewModel

import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.entity.MusicBucket
import com.protone.api.todayDate
import com.protone.seenn.Medias
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import com.protone.seenn.database.userConfig

class SplashViewModel : BaseViewModel() {

    sealed class SplashEvent {
        object InitConfig : ViewEvent
        object UpdateMedia : ViewEvent
    }

    fun firstBootWork() {
        if (userConfig.isFirstBoot) {
            DatabaseHelper.instance.musicBucketDAOBridge.addMusicBucketAsync(
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
            DatabaseHelper.instance.musicDAOBridge.insertMusicMultiAsync(Medias.music)
            userConfig.apply {
                isFirstBoot = false
                lastMusicBucket = R.string.all_music.getString()
                playedMusicPosition = -1
            }
        }
    }
}