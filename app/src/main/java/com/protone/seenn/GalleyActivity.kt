package com.protone.seenn

import android.content.Intent
import com.protone.api.context.deleteMedia
import com.protone.api.context.renameMedia
import com.protone.api.context.showFailedToast
import com.protone.api.json.toJson
import com.protone.mediamodle.Galley
import com.protone.seen.GalleySeen
import com.protone.seen.dialog.RenameDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class GalleyActivity : BaseActivity<GalleySeen>() {

    companion object {
        @JvmStatic
        val BUCKET = "GalleyBucket"

        @JvmStatic
        val CUSTOM = "CustomChoose"
    }

    override suspend fun main() {
        val galleySeen = GalleySeen(this)

        setContentSeen(galleySeen)

        galleySeen.initPager(Galley.photo, Galley.video)

        galleySeen.chooseData.observe(this) {
            if (it.size > 0) {
                galleySeen.setOptionButton(true)
            } else galleySeen.setOptionButton(false)
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                        }
                        else -> {
                        }
                    }
                }
                galleySeen.viewEvent.onReceive {
                    when (it) {
                        GalleySeen.Touch.Finish -> {
                            finish()
                        }
                        GalleySeen.Touch.ShowOptionMenu -> {
                            galleySeen.showPop()
                        }
                        GalleySeen.Touch.MOVE_TO -> {
                        }
                        GalleySeen.Touch.RENAME -> galleySeen.rename()
                        GalleySeen.Touch.ADD_CATE -> {
                        }
                        GalleySeen.Touch.SELECT_ALL -> {
                        }
                        GalleySeen.Touch.DELETE -> galleySeen.delete()
                        GalleySeen.Touch.IntoBOX -> {
                            startActivity(
                                Intent(
                                    this@GalleyActivity,
                                    PictureBoxActivity::class.java
                                ).apply {
                                    galleySeen.chooseData()
                                        ?.let { d -> putExtra(CUSTOM, d.toJson()) }
                                })
                        }
                    }
                }
            }
        }
    }

    private fun GalleySeen.rename() {
        chooseData.value?.onEach {
            RenameDialog(context, it.name) { name ->
                renameMedia(name, it.uri) { result ->
                    if (result) {
                        it.name = name
                    } else showFailedToast()
                }
            }
        }
    }

    private suspend fun GalleySeen.delete() = withContext(Dispatchers.IO) {
        chooseData.value?.onEach {
            deleteMedia(it.uri) { result ->
                if (result) {
                    Galley.deleteMedia(it.isVideo, it)
                }
            }
        }
    }

    private fun GalleySeen.chooseData() = chooseData.value
}