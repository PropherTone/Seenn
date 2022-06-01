package com.protone.seenn

import android.transition.TransitionManager
import com.protone.api.context.intent
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.mediamodle.Medias
import com.protone.seen.MainSeen
import com.protone.seenn.service.MusicControllerIMP
import com.protone.seenn.service.WorkService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class MainActivity : BaseActivity<MainSeen>() {

    override suspend fun main() {
        val mainSeen = MainSeen(this)
        setContentSeen(mainSeen)
        mainSeen.beginTransition()
        val musicController = MusicControllerIMP(mainSeen.musicController)
        bindMusicService {
            musicController.setMusicList(Medias.music)
            musicController.setBinder(this,binder)
        }

        Medias.mediaLive.observe(this) { code->
            if (code == Medias.AUDIO_UPDATED) {
                musicController.refresh()
            } else {
                mainSeen.refreshModelList()
            }
        }

        doOnFinish {
            DataBaseDAOHelper.shutdownNow()
            stopService(WorkService::class.intent)
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