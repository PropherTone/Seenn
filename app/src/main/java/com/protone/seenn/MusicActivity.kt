package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.MUSIC_PAUSE
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.intent
import com.protone.api.json.toUri
import com.protone.database.room.dao.MusicBucketDAOHelper
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.musicBroadCastManager
import com.protone.seen.MusicSeen
import com.protone.seen.adapter.MusicBucketAdapter
import com.protone.seen.adapter.MusicListAdapter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine

class MusicActivity : BaseActivity<MusicSeen>() {

    private var cacheMusicBucketName = "ALL"

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        setContentSeen(musicSeen)

        bindMusicService { }
        musicSeen.apply {
            initSeen()
            mbClickCallBack { name ->
                cacheMusicBucketName = name
            }
            mlClickCallBack { position ->
                binder.setDate(Galley.music)
                binder.setMusicPosition(position)
            }
        }


        bindMusicService {
            setMusicList(Galley.music)
            binder.getData().apply {
                musicSeen.musicName = name
                musicSeen.icon = albumUri
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
                            ).data?.apply {
                                musicSeen.addBucket(MusicBucket().apply {
                                    getStringExtra(AddBucketActivity.BUCKET_NAME)?.let {
                                        name = it
                                    }
                                    getStringExtra(AddBucketActivity.BUCKET_ICON)?.let {
                                        icon = it.toUri()
                                    }
                                    getStringExtra(AddBucketActivity.BUCKET_DETAIL)?.let {
                                        detail = it
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun MusicSeen.initSeen() {
        initList(suspendCancellableCoroutine { co ->
            MusicBucketDAOHelper.getAllMusicBucket { list ->
                list?.let {
                    co.resumeWith(Result.success(it as MutableList<MusicBucket>))
                }
                co.cancel()
            }
        }, Galley.music)

        initSmallMusic({
            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PLAY))
        }, {
            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PAUSE))
        })
    }

    private suspend fun MusicSeen.addBucket(musicBucket: MusicBucket) {
        MusicBucketDAOHelper.addMusicBucketWithCallBack(musicBucket) {
            launch {
                (binding.musicBucket.adapter as MusicBucketAdapter).addBucket(musicBucket)
            }
        }
    }

    private fun MusicSeen.observeMusicUpdate() {
        Galley.musicState.observe(this@MusicActivity) {
            musicName = it.name
            icon = it.albumUri
        }
    }
}