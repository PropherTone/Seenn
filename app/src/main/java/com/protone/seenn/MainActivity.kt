package com.protone.seenn

import android.content.Context
import android.content.Intent
import android.transition.TransitionManager
import android.util.Log
import com.protone.api.context.MUSIC_NEXT
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.MUSIC_PREVIOUS
import com.protone.api.context.intent
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.musicBroadCastManager
import com.protone.seen.MainSeen
import com.protone.seen.Progress
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class MainActivity : BaseActivity<MainSeen>() {

    override suspend fun main() {
        val mainSeen = MainSeen(this)
        setContentSeen(mainSeen)
        mainSeen.beginTransition()

        bindMusicService {
            setMusicList(Galley.music)
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

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {

                        }
                        else -> {}
                    }
                }
                mainSeen.viewEvent.onReceive {
                    when (it) {
                        MainSeen.Touch.GALLEY -> {
                            startActivity(GalleyActivity::class.intent)
//                            bindService(
//                                NoteSyncService::class.intent.apply {
//                                    action = "SERVER"
//                                },
//                                object : IServerConnection() {
//                                    override fun onServiceConnected(
//                                        p0: ComponentName?,
//                                        p1: IBinder?
//                                    ) {
//                                        super.onServiceConnected(p0, p1)
//                                        (p1 as NoteSyncService.SyncBinder).connect(
//                                            port = 6666,
//                                            statesString = object : CloudStates<String> {
//                                                override fun success() {
//                                                    Log.d(TAG, "success: ")
//                                                }
//
//                                                override fun failed(msg: String) {
//                                                    Log.d(TAG, "failed: ")
//                                                }
//
//                                                override fun successMsg(arg: String) {
//                                                    Log.d(TAG, "successMsg: ${arg.toJson()}")
//                                                }
//
//                                            })
//                                    }
//                                },
//                                BIND_AUTO_CREATE
//                            )
                        }
                        MainSeen.Touch.MUSIC -> startActivity(MusicActivity::class.intent)
                        MainSeen.Touch.NOTE -> startActivity(NoteEditActivity::class.intent)
                        MainSeen.Touch.PlayMusic -> musicBroadCastManager.sendBroadcast(
                            Intent().setAction(
                                MUSIC_PLAY
                            )
                        )
                        MainSeen.Touch.PauseMusic -> musicBroadCastManager.sendBroadcast(
                            Intent().setAction(
                                MUSIC_PLAY
                            )
                        )
                        MainSeen.Touch.PreviousMusic -> musicBroadCastManager.sendBroadcast(
                            Intent().setAction(
                                MUSIC_PREVIOUS
                            )
                        )
                        MainSeen.Touch.NextMusic -> musicBroadCastManager.sendBroadcast(
                            Intent().setAction(
                                MUSIC_NEXT
                            )
                        )

                        MainSeen.Touch.PauseVideo -> {}
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
        binder.getPlayState().observe(this@MainActivity){
            isPlaying = it
        }
    }

    private fun MainSeen.setAndUpdateDuration() {
        Galley.musicState.observe(this@MainActivity) {
            duration = it.duration
            musicName = it.name
            icon = it.albumUri
            isPlaying = it.isPlaying
        }
        updateDuration()
    }

    private fun MainSeen.musicSeek() {
        musicSeek(object : Progress {
            override fun getProgress(position: Long) {
                binder.seekTo(position)
            }
        })
    }
}