package com.protone.seenn

import android.content.Intent
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

    private lateinit var cacheMusicBucketName: String

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        cacheMusicBucketName = userConfig.playedMusicBucket

        setContentSeen(musicSeen)

        musicSeen.initSeen()

        bindMusicService {
            musicReceiver = object : MusicReceiver() {
                override fun play() {}

                override fun pause() {}

                override fun finish() {}

                override fun previous() {
                    musicSeen.playPosition()
                }

                override fun next() {
                    musicSeen.playPosition()
                }

            }
            setMusicList(Galley.music)
            binder.getData().apply {
                musicSeen.musicName = name
                musicSeen.icon = albumUri
            }
            musicSeen.isPlaying = binder.getPlayState().value == true
            musicSeen.playPosition()
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
                        MusicSeen.Event.Play -> musicBroadCastManager.sendBroadcast(
                            Intent().setAction(
                                MUSIC_PLAY
                            )
                        )
                        MusicSeen.Event.Finish -> cancel()
                        MusicSeen.Event.AddList -> startActivity(AddMusic2BucketActivity::class.intent.also {
                            it.putExtra("BUCKET",musicSeen.bucket)
                        })
                    }
                }

            }
        }

    }

    private suspend fun MusicSeen.initSeen() {
        initList(
            suspendCancellableCoroutine { co ->
                DataBaseDAOHelper.getAllMusicBucket { list ->
                    list?.let { lm ->
                        co.resumeWith(Result.success(lm as MutableList<MusicBucket>))
                    }
                    co.cancel()
                }
            },
            Galley.musicBucket[userConfig.playedMusicBucket] ?: Galley.music,
            userConfig.playedMusicBucket
        )
        mbClickCallBack { name ->
            cacheMusicBucketName = name
            hideBucket()
            updateMusicList(Galley.musicBucket[cacheMusicBucketName] ?: mutableListOf())
        }
        mlClickCallBack { position ->
            binder.setDate(Galley.musicBucket[cacheMusicBucketName] ?: mutableListOf())
            binder.setPlayMusicPosition(position)
            userConfig.playedMusicBucket = cacheMusicBucketName
        }

        initSmallMusic()
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
        }
        binder.getPlayState().observe(this@MusicActivity) {
            isPlaying = it
        }
    }
}