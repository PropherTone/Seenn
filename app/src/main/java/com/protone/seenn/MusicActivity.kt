package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.intent
import com.protone.database.room.dao.MusicBucketDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.musicBroadCastManager
import com.protone.seen.MusicSeen
import com.protone.seen.adapter.MusicBucketAdapter
import com.protone.seen.adapter.MusicListAdapter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class MusicActivity : BaseActivity<MusicSeen>() {

    override suspend fun main() {
        val musicSeen = MusicSeen(this)

        setContentSeen(musicSeen)

        musicSeen.initSeen()

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
                                        icon = it
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

    private fun MusicSeen.initSeen() {
        MusicBucketDAOHelper.getAllMusicBucket { list ->
            list?.let {
                runOnUiThread {
                    binding.musicBucket.apply {
                        layoutManager = LinearLayoutManager(this@MusicActivity)
                        adapter = MusicBucketAdapter(
                            this@MusicActivity,
                            it as MutableList<MusicBucket>
                        )
                    }
                }
            }
        }

        binding.musicMusicList.apply {
            layoutManager = LinearLayoutManager(this@MusicActivity)
            adapter = MusicListAdapter(this@MusicActivity).apply {
                musicList = Galley.music
            }
        }

        binding.mySmallMusicPlayer.apply {
            playMusic = {
                musicBroadCastManager.sendBroadcast(Intent().setAction("PlayMusic"))
            }
            pauseMusic = {
                musicBroadCastManager.sendBroadcast(Intent().setAction("PauseMusic"))
            }
        }
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