package com.protone.worker.viewModel

import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.entity.MusicBucket
import com.protone.api.todayDate
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import com.protone.worker.media.scanAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplashViewModel : BaseViewModel() {

    sealed class SplashEvent {
        object InitConfig : ViewEvent
        object UpdateMedia : ViewEvent
    }

    suspend fun firstBootWork() {
        if (userConfig.isFirstBoot) {
            DatabaseHelper.instance.run {
                withContext(Dispatchers.IO){
                    musicDAOBridge.insertMusicMulti(scanAudio { _, _ -> })
                }
                val allMusicRs = musicDAOBridge.getAllMusicRs() ?: return
                musicBucketDAOBridge.addMusicBucketAsync(
                    MusicBucket(
                        R.string.all_music.getString(),
                        if (allMusicRs.isNotEmpty()) allMusicRs[0].uri.saveToFile(
                            R.string.all_music.getString(),
                            R.string.music_bucket.getString()
                        ) else null,
                        allMusicRs.size,
                        null,
                        todayDate("yyyy/MM/dd")
                    )
                )
                musicWithMusicBucketDAOBridge.insertMusicMultiAsyncWithBucket(
                    R.string.all_galley.getString(),
                    allMusicRs
                )
            }
            userConfig.apply {
                isFirstBoot = false
                lastMusicBucket = R.string.all_music.getString()
                playedMusicPosition = -1
            }
        }
    }
}