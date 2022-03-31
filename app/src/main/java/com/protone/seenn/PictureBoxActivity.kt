package com.protone.seenn

import android.net.Uri
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.json.jsonToList
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.PictureBoxSeen
import com.protone.seen.adapter.PictureBoxAdapter
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
                            pictureBoxSeen.initPictureBox(
                                intent.getStringExtra(GalleyActivity.CUSTOM)
                                    ?.jsonToList(GalleyMedia::class.java) as MutableList<GalleyMedia>
                            )
                        }
                        Event.OnResume -> {
                        }
                        Event.OnStop -> {
                        }
                        else -> {
                        }
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