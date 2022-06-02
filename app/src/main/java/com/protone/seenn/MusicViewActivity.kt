package com.protone.seenn

import com.protone.seen.MusicViewSeen
import com.protone.seenn.viewModel.MusicControllerIMP
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class MusicViewActivity : BaseActivity<MusicViewSeen>() {

    override suspend fun main() {
        val musicViewSeen = MusicViewSeen(this)
        setContentSeen(musicViewSeen)
        val musicController = MusicControllerIMP(musicViewSeen.controller)
        bindMusicService {
            musicController.setBinder(this, binder)
            musicController.refresh()
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