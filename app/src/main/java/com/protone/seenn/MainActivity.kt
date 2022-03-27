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
//                           val startActivityForResult = startActivityForResult(
//                                ActivityResultContracts.StartActivityForResult(),
//                                Intent(
//                                    Intent.ACTION_PICK,
//                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//                                )
//                            )
//                            if (startActivityForResult.resultCode == RESULT_OK) {
//                                startActivityForResult.data?.data?.let { it1 ->
//                                    mainSeen.setVideo(
//                                        it1
//                                    )
//                                }
//                            }
//                            val startActivityForResult = startActivityForResult(
//                                ActivityResultContracts.StartActivityForResult(),
//                                NoteSyncActivity::class.intent
//                            )
//
//                            if(startActivityForResult.resultCode == RESULT_OK){
//                                Log.d(TAG, "main: ${startActivityForResult.data?.getStringExtra("ttt")}")
//                            }
//                            val galleyFilter = MediaFilter()
//                            galleyFilter.scanAudio(object : MediaFilter.Filter<MutableList<MediaFilter.AudioDetail>>{
//                                override fun offer(arg: MutableList<MediaFilter.AudioDetail>) {
//                                    Log.d(TAG, "offer: ${arg[0]}")
//                                }
//
//                            })
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
                        MainSeen.Touch.MUSIC -> {
                            startActivity(MusicActivity::class.intent)
                        }
                        MainSeen.Touch.NOTE -> {}
                        MainSeen.Touch.PlayMusic -> {
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PLAY))
                        }
                        MainSeen.Touch.PauseMusic -> {
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PLAY))
                        }
                        MainSeen.Touch.PreviousMusic -> {
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PREVIOUS))
                        }
                        MainSeen.Touch.NextMusic -> {
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_NEXT))
                        }
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
    }

    private fun MainSeen.setAndUpdateDuration() {
        Galley.musicState.observe(this@MainActivity) {
            Log.d(TAG, "setAndUpdateDuration: $it")
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