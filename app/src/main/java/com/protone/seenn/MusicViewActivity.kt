package com.protone.seenn

import com.protone.database.room.entity.Music
import com.protone.database.sp.config.userConfig
import com.protone.seen.MusicViewSeen
import com.protone.seen.adapter.TransparentPlayListAdapter
import com.protone.seenn.viewModel.MusicControllerIMP
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class MusicViewActivity : BaseActivity<MusicViewSeen>() {

    override suspend fun main() {
        val musicViewSeen = MusicViewSeen(this)
        setContentSeen(musicViewSeen)
        val musicController = MusicControllerIMP(musicViewSeen.controller)
        bindMusicService {
            musicController.setBinder(this, binder) {
                userConfig.musicLoopMode = it
            }
            musicController.refresh()
            musicController.setLoopMode(userConfig.musicLoopMode)
            musicViewSeen.initPlayList(
                binder.getPlayList(), musicController.getPlayingMusic(),
                object : TransparentPlayListAdapter.OnPlayListClk {
                    override fun onClk(music: Music) {
                        musicController.play(music)
                    }
                })
        }
        doOnFinish {
            musicController.finish()
        }
        while (isActive) {
            select<Unit> {
                musicViewSeen.viewEvent.onReceive {
                    when (it) {
                        MusicViewSeen.MusicEvent.Finish -> finish()
                    }
                }
            }
        }
    }
}