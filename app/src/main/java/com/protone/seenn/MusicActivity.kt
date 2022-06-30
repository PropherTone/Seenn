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
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.seen.MusicSeen
import com.protone.seenn.broadcast.musicBroadCastManager
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.viewModel.MusicControllerIMP
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.suspendCoroutine

class MusicActivity : BaseActivity<MusicSeen>() {

    private lateinit var musicController: MusicControllerIMP

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        musicSeen.bucket = userConfig.lastMusicBucket
        musicController = MusicControllerIMP(musicSeen.musicController)
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }
        setContentSeen(musicSeen)

        musicSeen.initSeen()
        musicSeen.setBucket()
        bindMusicService {
            this@MusicActivity.musicController.setBinder(this@MusicActivity, binder, onPlaying = {
                musicSeen.playPosition(it)
            })
            musicController.setMusicList(Medias.musicBucket[musicSeen.bucket] ?: Medias.music)
        }

        Medias.mediaLive.observe(this) {
            if (it == Medias.AUDIO_UPDATED) {
                workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
            }
        }

        doOnFinish {
            musicSeen.clearMer()
            musicController.finish()
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
                        MusicSeen.Event.RefreshBucket -> musicSeen.refreshBucket()

                    }
                }

            }
        }
    }

    private suspend fun MusicSeen.initSeen() {
        val list = DataBaseDAOHelper.getAllMusicBucketRs()
        val buckets = suspendCoroutine<MutableList<MusicBucket>> { co ->
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
                    if (re) co.resumeWith(Result.success(DataBaseDAOHelper.getAllMusicBucketRs() as MutableList<MusicBucket>))
                }
            } else co.resumeWith(Result.success(list as MutableList<MusicBucket>))
        }

        initList(
            buckets,
            Medias.musicBucket[userConfig.lastMusicBucket] ?: Medias.music,
            userConfig.lastMusicBucket
        )

        mbClickCallBack { name ->
            bucket = name
            hideBucket()
            updateBucket()
        }
        mlClickCallBack { music ->
            if (userConfig.lastMusicBucket != bucket) {
                this@MusicActivity.musicController.setMusicList(
                    Medias.musicBucket[bucket] ?: mutableListOf()
                )
            }
            this@MusicActivity.musicController.play(music)
            userConfig.lastMusicBucket = bucket
        }

        Medias.musicBucketLive.observe(this@MusicActivity) {
            offer(MusicSeen.Event.RefreshBucket)
        }
    }

    private fun MusicSeen.updateBucket() {
        onUiThread {
            setBucket()
            updateMusicList(Medias.musicBucket[bucket] ?: mutableListOf())
        }
    }

    private suspend fun MusicSeen.refreshBucket() = withContext(Dispatchers.IO) {
        DataBaseDAOHelper.getMusicBucketByName(bucket)
            ?.let { b -> refreshBucket(b) }
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
                val re = DataBaseDAOHelper.deleteMusicBucketRs(musicBucket)
                if (re) {
                    toast(getString(R.string.success))
                    workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                    bucket = getString(R.string.all_music)
                    updateBucket()
                } else {
                    toast(getString(R.string.failed_msg))
                    addBucketNoCheck(musicBucket)
                }
            } else {
                toast(getString(R.string.failed_msg))
            }
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