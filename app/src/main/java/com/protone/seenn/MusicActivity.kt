package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.intent
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.MusicReceiver
import com.protone.mediamodle.media.musicBroadCastManager
import com.protone.seen.MusicSeen
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

class MusicActivity : BaseActivity<MusicSeen>() {

    private var cacheMusicBucketName = "ALL"

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        setContentSeen(musicSeen)

        musicSeen.apply {
            initSeen()
            mbClickCallBack { name ->
                cacheMusicBucketName = name
                musicSeen.hideBucket()
            }
            mlClickCallBack { position ->
                binder.setDate(Galley.music)
                binder.setMusicPosition(position)
                userConfig.playedMusicBucket = cacheMusicBucketName
            }
        }

        bindMusicService {
            musicReceiver = object : MusicReceiver() {
                override fun play() {
                    musicSeen.isPlaying = true
                    Log.d(TAG, "play: ")
                }

                override fun pause() {
                    musicSeen.isPlaying = false
                    Log.d(TAG, "pause: ")
                }

                override fun finish() {

                }

                override fun previous() {
                    musicSeen.playPosition()
                }

                override fun next() {
                    musicSeen.playPosition()
                }

            }
            musicSeen.playPosition()
        }

        bindMusicService {
            setMusicList(Galley.music)
            binder.getData().apply {
                musicSeen.musicName = name
                musicSeen.icon = albumUri
                musicSeen.isPlaying = isPlaying
            }
            musicSeen.observeMusicUpdate()
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                musicSeen.viewEvent.onReceive { event ->
                    when (event) {
                        MusicSeen.Event.AddBucket -> {
                            startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                AddBucketActivity::class.intent
                            ).apply {
                                resultCode.let { code ->
                                    when (code) {
                                        RESULT_OK -> {
                                            data?.getStringExtra(
                                                AddBucketActivity.BUCKET_NAME
                                            )?.let {
                                                musicSeen.addBucket(
                                                    it
                                                )
                                            }
                                        }
                                        RESULT_CANCELED -> {
                                            toast(getString(R.string.cancel))
                                        }
                                    }
                                }
                            }
                        }
                        MusicSeen.Event.Finish -> cancel()
                    }
                }

            }
        }

    }

    private suspend fun MusicSeen.initSeen() {
        Galley.musicBucket[userConfig.playedMusicBucket]?.let {
            initList(suspendCancellableCoroutine { co ->
                DataBaseDAOHelper.getAllMusicBucket { list ->
                    list?.let { lm ->
                        co.resumeWith(Result.success(lm as MutableList<MusicBucket>))
                    }
                    co.cancel()
                }
            }, it)
        }

        initSmallMusic({
            musicBroadCastManager.sendBroadcast(
                Intent().setAction(
                    MUSIC_PLAY
                )
            )
        }, {
            musicBroadCastManager.sendBroadcast(
                Intent().setAction(
                    MUSIC_PLAY
                )
            )
        })
    }

    private fun MusicSeen.playPosition() {
        playPosition(binder.getPlayPosition())
    }

    private suspend fun MusicSeen.addBucket(name: String) = launch(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicBucketByName(name)?.let { addBucket(it) }
    }

    private fun MusicSeen.observeMusicUpdate() {
        Galley.musicState.observe(this@MusicActivity) {
            musicName = it.name
            icon = it.albumUri
//            isPlaying = it.isPlaying
        }
    }
}