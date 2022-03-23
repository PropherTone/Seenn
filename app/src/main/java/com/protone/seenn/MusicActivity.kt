package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.TAG
import com.protone.api.context.intent
import com.protone.database.room.dao.MusicBucketDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.musicBroadCastManager
import com.protone.seen.MusicSeen
import com.protone.seen.adapter.MusicBucketAdapter
import kotlinx.coroutines.isActive
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
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                musicSeen.viewEvent.onReceive {
                    when (it) {
                        MusicSeen.Event.AddBucket -> {
                            val result = startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                AddBucketActivity::class.intent
                            )
                            val stringExtra = result.data?.getStringExtra("BUCKET_NAME")
                            Log.d(TAG, "main: $stringExtra")
                            stringExtra?.let { name->
                                musicSeen.addBucket(MusicBucket(name,null))
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
                    binding.musicBucket.layoutManager = LinearLayoutManager(this@MusicActivity)
                    binding.musicBucket.adapter = MusicBucketAdapter(
                        this@MusicActivity,
                        it as MutableList<MusicBucket>
                    )
                }
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

    private fun MusicSeen.addBucket(musicBucket: MusicBucket) {
        MusicBucketDAOHelper.addMusicBucketWithCallBack(musicBucket){
            (binding.musicBucket.adapter as MusicBucketAdapter).addBucket(musicBucket)
        }
    }
}