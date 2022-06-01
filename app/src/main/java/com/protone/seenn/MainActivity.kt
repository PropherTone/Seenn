package com.protone.seenn

import android.content.Intent
import android.transition.TransitionManager
import com.protone.api.context.MUSIC_NEXT
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.MUSIC_PREVIOUS
import com.protone.api.context.intent
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.mediamodle.Medias
import com.protone.seen.MainSeen
import com.protone.seen.customView.ColorfulProgressBar
import com.protone.seenn.broadcast.musicBroadCastManager
import com.protone.seenn.service.WorkService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class MainActivity : BaseActivity<MainSeen>() {

    override suspend fun main() {
        val mainSeen = MainSeen(this)
        setContentSeen(mainSeen)
        mainSeen.beginTransition()

        bindMusicService {
            setMusicList(Medias.music)
            mainSeen.apply {
                binder.getData().let {
                    musicName = it.name
                    icon = it.albumUri
                    duration = it.duration
                    isPlaying = it.isPlaying
                }
            }
            mainSeen.setAndUpdateDuration()
        }

        Medias.mediaLive.observe(this) { code->
            if (code == Medias.AUDIO_UPDATED) {
                mainSeen.apply {
                    binder.getData().let {
                        musicName = it.name
                        icon = it.albumUri
                        duration = it.duration
                        isPlaying = it.isPlaying
                    }
                }
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
                        MainSeen.Touch.PlayMusic ->
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PLAY))
                        MainSeen.Touch.PauseMusic ->
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PLAY))
                        MainSeen.Touch.PreviousMusic ->
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PREVIOUS))
                        MainSeen.Touch.NextMusic ->
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_NEXT))
                        MainSeen.Touch.ConfigUser -> startActivity(UserConfigActivity::class.intent)
                    }
                }
            }
        }
    }

    private fun MainSeen.beginTransition() {
        TransitionManager.beginDelayedTransition(group)
    }

    private fun MainSeen.updateDuration() {
        musicSeek()
        binder.getPosition().observe(this@MainActivity) {
            progress = it
        }
        binder.getPlayState().observe(this@MainActivity) {
            isPlaying = it
        }
    }

    private fun MainSeen.setAndUpdateDuration() {
        Medias.musicState.observe(this@MainActivity) {
            duration = it.duration
            musicName = it.name
            icon = it.albumUri
        }
        updateDuration()
    }

    private fun MainSeen.musicSeek() {
        musicSeek(object : ColorfulProgressBar.Progress {
            override fun getProgress(position: Long) {
                binder.seekTo(position)
            }
        })
    }
}