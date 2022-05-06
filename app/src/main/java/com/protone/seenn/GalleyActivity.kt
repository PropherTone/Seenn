package com.protone.seenn

import android.content.Intent
import com.protone.api.context.deleteMedia
import com.protone.api.context.intent
import com.protone.api.context.renameMedia
import com.protone.api.context.showFailedToast
import com.protone.api.getFileMimeType
import com.protone.api.getFileName
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyBucket
import com.protone.mediamodle.Galley
import com.protone.seen.GalleySeen
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.popWindows.ColorfulPopWindow
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
            initPager(
                Galley.photo, Galley.video, chooseType,
                { gm, isVideo ->
                    startActivity(GalleyViewActivity::class.intent.apply {
                        putExtra(GalleyViewActivity.MEDIA, gm.toJson())
                        putExtra(GalleyViewActivity.TYPE, isVideo)
                    })
                },
            ) { isVideo ->
                TitleDialog(this@GalleyActivity, "名称", "") {
                    DataBaseDAOHelper.insertGalleyBucketCB(GalleyBucket(it, isVideo)) { re, name ->
                        if (re) addBucket(name) else toast(getString(R.string.failed_msg))
                    }
                }
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
                        GalleySeen.Touch.MOVE_TO -> galleySeen.startListPop()
                        GalleySeen.Touch.RENAME -> galleySeen.rename()
                        GalleySeen.Touch.ADD_CATE -> galleySeen.addCate()
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
                        GalleySeen.Touch.ShowPop -> galleySeen.showPop(galleySeen.chooseData()?.size ?: 0 > 0)
                    }
                }
            }
        }
    }

    private fun GalleySeen.rename() = chooseData.value?.onEach {
        if (it.path == null) {
            toast(getString(R.string.not_supported))
            return@onEach
        }
        val mimeType = it.path!!.getFileName().getFileMimeType()
        TitleDialog(
            context,
            getString(R.string.rename),
            it.path!!.getFileName().replace(mimeType, "")
        ) { name ->
            renameMedia(name + mimeType, it.uri) { result ->
                if (result) {
                    it.name = name + mimeType
                } else showFailedToast()
            }
        }
    }


    private suspend fun GalleySeen.delete() = withContext(Dispatchers.IO) {
        chooseData.value?.onEach {
            deleteMedia(it.uri) { result ->
                if (result) Galley.deleteMedia(it.isVideo, it)
            }
        }
    }

    private fun GalleySeen.addCate() = TitleDialog(context, getString(R.string.rename), "") { re ->
        if (re.isEmpty()) {
            toast("请输入内容")
            return@TitleDialog
        }
        chooseData.value?.let { list ->
            list.forEach { gm ->
                gm.also { g ->
                    if (g.cate == null) g.cate = mutableListOf()
                    (g.cate as MutableList).add(re)
                }
            }
            DataBaseDAOHelper.insertSignedMediaMulti(list)
        }
    }

    private suspend fun GalleySeen.startListPop() {
        val pop = ColorfulPopWindow(this@GalleyActivity)
        pop.startListPopup(
            getBar(),
            withContext(Dispatchers.IO) {
                val list = mutableListOf<String>()
                DataBaseDAOHelper.getALLGalleyBucket()?.forEach {
                    list.add(it.type)
                }
                list
            }) { re ->
            chooseData.value?.let { list ->
                list.forEach {
                    if (re != null) {
                        if (it.type == null) it.type = mutableListOf()
                        if (it.type?.contains(re) == false)
                            (it.type as MutableList<String>).add(re)
                    } else toast(getString(R.string.none))
                }
                DataBaseDAOHelper.insertSignedMediaMulti(list)
            }
            pop.dismiss()
        }
    }

    private fun GalleySeen.chooseData() = chooseData.value
}