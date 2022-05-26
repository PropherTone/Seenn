package com.protone.seenn

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.api.context.intent
import com.protone.api.context.onUiThread
import com.protone.api.toBitmapByteArray
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Medias
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seen.MusicSeen
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.broadcast.musicBroadCastManager
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.suspendCoroutine

class MusicActivity : BaseActivity<MusicSeen>() {

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        musicSeen.bucket = userConfig.playedMusicBucket

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
            setMusicList(Medias.musicBucket[musicSeen.bucket] ?: Medias.music)
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
                                        musicSeen.bucket
                                    )
                                }).also { re ->
                                if (re.resultCode == RESULT_OK)
                                    re.data?.getStringExtra(AddBucketActivity.BUCKET_NAME)
                                        ?.let {
                                            musicSeen.bucket = it
                                            musicSeen.performListClick(musicSeen.bucket)
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
                            if (Medias.music.size > 0) Medias.music[0].uri.toBitmapByteArray() else null,
                            Medias.music.size,
                            null,
                            todayDate("yyyy/MM/dd")
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
            Medias.musicBucket[userConfig.playedMusicBucket] ?: Medias.music,
            userConfig.playedMusicBucket
        )

        mbClickCallBack { name ->
            bucket = name
            hideBucket()
            updateBucket()
        }
        mlClickCallBack { position ->
            binder.setDate(Medias.musicBucket[bucket] ?: mutableListOf())
            binder.setPlayMusicPosition(position)
            userConfig.playedMusicBucket = bucket
        }

        Medias.musicBucketLive.observe(this@MusicActivity) {
            offer(MusicSeen.Event.RefreshBucket)
        }

        initSmallMusic()
    }

    private fun MusicSeen.updateBucket(){
        setBucket()
        updateMusicList(Medias.musicBucket[bucket] ?: mutableListOf())
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
            DataBaseDAOHelper.getMusicBucketByName(bucket)
        }
        if (musicBucket != null) {
            if (deleteBucket(musicBucket)) {
                DataBaseDAOHelper.deleteMusicBucketCB(musicBucket) { re ->
                    onUiThread {
                        if (re) {
                            toast(getString(R.string.success))
                            workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                            bucket = getString(R.string.all_music)
                            updateBucket()
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
        Medias.musicState.observe(this@MusicActivity) {
            if (musicName != it.name) musicName = it.name
            if (icon != it.albumUri) icon = it.albumUri
        }
        binder.getPlayState().observe(this@MusicActivity) {
            isPlaying = it
        }
    }

    private fun MusicSeen.setBucket() = launch(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicBucketByName(bucket)?.let {
            setBucket(
                it.icon,
                it.name,
                if (it.date != null && it.detail != null) "${it.date} ${it.detail}" else getString(R.string.none)
            )
        }
    }

    private fun MusicSeen.compareName(): Boolean {
        if (bucket == "" || bucket == getString(R.string.all_music)) {
            toast(getString(R.string.none))
            return true
        }
        return false
    }
}