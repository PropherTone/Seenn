package com.protone.seenn

import android.content.Intent
import android.util.Log
import com.protone.api.context.deleteMedia
import com.protone.api.context.intent
import com.protone.api.context.renameMedia
import com.protone.api.context.showFailedToast
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.api.toBitmapByteArray
import com.protone.api.toMediaBitmapByteArray
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

        @JvmStatic
        val CHOOSE_MODE = "ChooseData"
    }

    private var chooseMode = false

    private var chooseType = ""

    override suspend fun main() {
        val galleySeen = GalleySeen(this)

        setContentSeen(galleySeen)

        intent.getStringExtra(CHOOSE_MODE)?.let {
            chooseMode = true
            chooseType = it
        }

        galleySeen.apply {
            initPager(Galley.photo, Galley.video, chooseType) { gm, isVideo ->
                startActivity(GalleyViewActivity::class.intent.apply {
                    putExtra(GalleyViewActivity.MEDIA, gm.toJson())
                    putExtra(GalleyViewActivity.TYPE, isVideo)
                })
            }
            chooseData.observe(this@GalleyActivity) {
                if (it.size > 0) {
                    galleySeen.setOptionButton(true)
                } else galleySeen.setOptionButton(false)
            }
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                galleySeen.viewEvent.onReceive {
                    when (it) {
                        GalleySeen.Touch.Finish -> finish()
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
                                    putExtra(
                                        CUSTOM,
                                        (galleySeen.chooseData() ?: Galley.allPhoto)?.toJson()
                                    )
                                })
                        }
                        GalleySeen.Touch.ConfirmChoose -> {
                            galleySeen.chooseData.value?.let { list ->
                                if (list.size > 0) {
                                    setResult(RESULT_OK, Intent().apply {
                                        putExtra("Uri", list[0].uri.toUriJson())
                                        putExtra("GalleyData", list[0].toJson())
                                    })
                                }
                            }
                            finish()
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