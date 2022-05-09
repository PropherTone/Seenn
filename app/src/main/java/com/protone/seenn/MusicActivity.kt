package com.protone.seenn

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.api.context.intent
import com.protone.api.context.onUiThread
import com.protone.api.toBitmapByteArray
import com.protone.api.todayTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.MusicReceiver
import com.protone.mediamodle.media.musicBroadCastManager
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seen.MusicSeen
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.suspendCoroutine

class MusicActivity : BaseActivity<MusicSeen>() {

    private lateinit var cacheMusicBucketName: String

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        cacheMusicBucketName = userConfig.playedMusicBucket

        setContentSeen(musicSeen)

        musicSeen.initSeen()
        musicSeen.setBucket()

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
            setMusicList(Galley.musicBucket[cacheMusicBucketName] ?: Galley.music)
            binder.getData().apply {
                musicSeen.musicName = name
                musicSeen.icon = albumUri
            }
            musicSeen.isPlaying = binder.getPlayState().value == true
            musicSeen.playPosition()
            musicSeen.observeMusicUpdate()
        }

        doOnFinish {
            musicSeen.clearMer()
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                musicSeen.viewEvent.onReceive { event ->
                    when (event) {
                        MusicSeen.Event.AddBucket -> {
                            startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                AddBucketActivity::class.intent
                            ).also { re ->
                                when (re.resultCode) {
                                    RESULT_OK -> re.data?.getStringExtra(AddBucketActivity.BUCKET_NAME)
                                        ?.let { musicSeen.addBucket(it) }
                                    RESULT_CANCELED -> toast(getString(R.string.cancel))
                                }
                            }
                        }
                        MusicSeen.Event.Play ->
                            musicBroadCastManager.sendBroadcast(Intent().setAction(MUSIC_PLAY))
                        MusicSeen.Event.Finish -> cancel()
                        MusicSeen.Event.AddList -> if (!musicSeen.compareName())
                            startActivity(PickMusicActivity::class.intent
                                .also { it.putExtra("BUCKET", musicSeen.bucket) })
                        MusicSeen.Event.Delete -> musicSeen.delete()
                        MusicSeen.Event.Edit -> if (!musicSeen.compareName())
                            startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                AddBucketActivity::class.intent.apply {
                                    putExtra(
                                        AddBucketActivity.BUCKET_NAME,
                                        musicSeen.getMusicBucketName()
                                    )
                                }).also { re ->
                                if (re.resultCode == RESULT_OK)
                                    re.data?.getStringExtra(AddBucketActivity.BUCKET_NAME)
                                        ?.let { name ->
                                            musicSeen.bucket = name
                                            musicSeen.offer(MusicSeen.Event.RefreshBucket)
                                        }
                            }
                        MusicSeen.Event.RefreshBucket-> musicSeen.refreshBucket()

                    }
                }

            }
        }
    }

    private suspend fun MusicSeen.initSeen() {
        val buckets = suspendCoroutine<MutableList<MusicBucket>> { co ->
            DataBaseDAOHelper.getAllMusicBucket { list ->
                if (list == null || list.isEmpty()) {
                    DataBaseDAOHelper.addMusicBucketWithCallBack(
                        MusicBucket(
                            getString(R.string.all_music),
                            if (Galley.music.size > 0) Galley.music[0].uri.toBitmapByteArray() else null,
                            Galley.music.size,
                            null,
                            todayTime("yyyy/MM/dd")
                        )
                    ) { re, _ ->
                        if (re) {
                            DataBaseDAOHelper.getAllMusicBucket {
                                co.resumeWith(Result.success(it as MutableList<MusicBucket>))
                            }
                        }
                    }
                } else {
                    co.resumeWith(Result.success(list as MutableList<MusicBucket>))
                }
            }
        }

        initList(
            buckets,
            Galley.musicBucket[userConfig.playedMusicBucket] ?: Galley.music,
            userConfig.playedMusicBucket
        )

        mbClickCallBack { name ->
            cacheMusicBucketName = name
            hideBucket()
            setBucket()
            updateMusicList(Galley.musicBucket[cacheMusicBucketName] ?: mutableListOf())
        }
        mlClickCallBack { position ->
            binder.setDate(Galley.musicBucket[cacheMusicBucketName] ?: mutableListOf())
            binder.setPlayMusicPosition(position)
            userConfig.playedMusicBucket = cacheMusicBucketName
        }

        Galley.musicBucketLive.observe(this@MusicActivity) {
            offer(MusicSeen.Event.RefreshBucket)
        }

        initSmallMusic()
    }

    private suspend fun MusicSeen.refreshBucket() = withContext(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicBucketByName(bucket)
            ?.let { b -> refreshBucket(b) }
    }


    private fun MusicSeen.playPosition() {
        playPosition(binder.getPlayPosition())
    }

    private suspend fun MusicSeen.addBucket(name: String) = launch(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicBucketByName(name)?.let { addBucket(it) }
    }

    private suspend fun MusicSeen.delete() {
        if (compareName()) return
        val musicBucket = withContext(Dispatchers.IO) {
            DataBaseDAOHelper.getMusicBucketByName(getMusicBucketName())
        }
        if (musicBucket != null) {
            if (deleteBucket(musicBucket)) {
                DataBaseDAOHelper.deleteMusicBucketCB(musicBucket) { re ->
                    onUiThread {
                        if (re) {
                            toast(getString(R.string.success))
                            workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                        } else {
                            toast(getString(R.string.failed_msg))
                            addBucketNoCheck(musicBucket)
                        }
                    }
                }
            } else {
                toast(getString(R.string.failed_msg))
            }
        }
    }

    private fun MusicSeen.observeMusicUpdate() {
        Galley.musicState.observe(this@MusicActivity) {
            if (musicName != it.name) musicName = it.name
            if (icon != it.albumUri) icon = it.albumUri
        }
        binder.getPlayState().observe(this@MusicActivity) {
            isPlaying = it
        }
    }

    private fun MusicSeen.setBucket() = launch(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicBucketByName(cacheMusicBucketName)?.let {
            setBucket(
                it.icon,
                it.name,
                if (it.date != null && it.detail != null) "${it.date} ${it.detail}" else getString(R.string.none)
            )
        }
    }

    private fun MusicSeen.compareName(): Boolean {
        val musicBucketName = getMusicBucketName()
        if (musicBucketName == "" || musicBucketName == getString(R.string.all_music)) {
            toast(getString(R.string.none))
            return true
        }
        return false
    }
}