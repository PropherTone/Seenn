package com.protone.seenn

import android.net.Uri
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.json.jsonToList
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.PictureBoxSeen
import com.protone.seen.adapter.PictureBoxAdapter
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select


class PictureBoxActivity : BaseActivity<PictureBoxSeen>() {

    @Suppress("UNCHECKED_CAST")
    override suspend fun main() {
        setTranslucentStatues()
        val pictureBoxSeen = PictureBoxSeen(this)

        setContentSeen(pictureBoxSeen)

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                            when (val data= IntentDataHolder.get()) {
                                null->finish()
                                is List<*> ->{
                                    if (data.isNotEmpty() && data[0] is GalleyMedia) {
                                        pictureBoxSeen.initPictureBox(
                                            data as MutableList<GalleyMedia>
                                        )
                                    }else finish()
                                }
                            }

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