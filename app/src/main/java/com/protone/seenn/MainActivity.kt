package com.protone.seenn

import android.content.Intent
import android.transition.TransitionManager
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Medias
import com.protone.seen.MainSeen
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.service.WorkService
import com.protone.seenn.viewModel.MusicControllerIMP
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select


/**
 * By ProTone
 *   2022/1/23
 */
class MainActivity : BaseActivity<MainSeen>() {

    override suspend fun main() {
        val mainSeen = MainSeen(this)
        setContentSeen(mainSeen)
        mainSeen.beginTransition()
        val musicController = MusicControllerIMP(mainSeen.musicController)
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }
        bindMusicService {
            musicController.setBinder(this, binder) {
                userConfig.musicLoopMode = it
            }
            musicController.setLoopMode(userConfig.musicLoopMode)
            Medias.musicBucket[userConfig.lastMusicBucket]?.let {
                musicController.setMusicList(it)
                musicController.refresh(
                    if (userConfig.lastMusic.isNotEmpty()) userConfig.lastMusic.toEntity(
                        Music::class.java
                    ) else binder.getPlayList()[0], userConfig.lastMusicProgress
                )
            }
        }

        Medias.mediaLive.observe(this) { code ->
            if (code == Medias.AUDIO_UPDATED) {
                musicController.refresh()
                workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
            } else {
                mainSeen.refreshModelList()
            }
        }

        doOnFinish {
            DataBaseDAOHelper.shutdownNow()
            stopService(WorkService::class.intent)
            userConfig.lastMusicProgress = musicController.getProgress() ?: 0L
            userConfig.lastMusic = binder.onMusicPlaying().value?.toJson() ?: ""
            musicController.finish()
        }
        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                            mainSeen.userName = userConfig.userName
                            mainSeen.userIcon = userConfig.userIcon
                        }
                        else -> {}
                    }
                }
                mainSeen.viewEvent.onReceive {
                    when (it) {
                        MainSeen.Touch.GALLEY -> startActivity(GalleyActivity::class.intent)
                        MainSeen.Touch.MUSIC -> if (userConfig.lockMusic == "")
                            startActivity(MusicActivity::class.intent) else toast(getString(R.string.locked))
                        MainSeen.Touch.NOTE -> if (userConfig.lockNote == "")
                            startActivity(NoteActivity::class.intent) else toast(getString(R.string.locked))
                        MainSeen.Touch.ConfigUser -> startActivity(UserConfigActivity::class.intent)
                    }
                }
            }
        }
    }

    private fun MainSeen.beginTransition() {
        TransitionManager.beginDelayedTransition(group)
    }
}