package com.protone.seenn

import com.protone.seen.PictureBoxSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class PictureBoxActivity : BaseActivity<PictureBoxSeen>() {

    override suspend fun main() {

        val pictureBoxSeen = PictureBoxSeen(this)

        setContentSeen(pictureBoxSeen)

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                        }
                        Event.OnResume -> {}
                        Event.OnStop -> {}
                        else -> {}
                    }
                }
                pictureBoxSeen.viewEvent.onReceive {
                    when (it) {
                        PictureBoxSeen.PictureBox.SelectPicList -> {

                        }
                    }
                }
            }
        }
    }

}