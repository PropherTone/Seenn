package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.deleteMedia
import com.protone.api.context.intent
import com.protone.api.context.renameMedia
import com.protone.api.context.showFailedToast
import com.protone.api.getFileMimeType
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.mediamodle.media.IGalleyFragment
import com.protone.seen.GalleySeen
import com.protone.seen.dialog.CateDialog
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.popWindows.ColorfulPopWindow
import com.protone.seenn.fragment.GalleyFragment
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class GalleyActivity : BaseActivity<GalleySeen>() {

    companion object {
        @JvmStatic
        val CUSTOM = "CustomChoose"

        @JvmStatic
        val CHOOSE_MODE = "ChooseData"

        @JvmStatic
        val URI = "Uri"

        @JvmStatic
        val GALLEY_DATA = "GalleyData"
    }

    private var chooseType = ""

    private val chooseData: MutableLiveData<MutableList<GalleyMedia>> =
        MutableLiveData<MutableList<GalleyMedia>>()

    private val iGalleyFragment = object : IGalleyFragment {
        override fun select(galleyMedia: MutableList<GalleyMedia>) {
            chooseData.postValue(galleyMedia)
        }

        override fun openView(galleyMedia: GalleyMedia, galley: String) {
            startActivity(GalleyViewActivity::class.intent.apply {
                putExtra(GalleyViewActivity.MEDIA, galleyMedia.toJson())
                putExtra(GalleyViewActivity.TYPE, galleyMedia.isVideo)
                putExtra(GalleyViewActivity.GALLEY, galley)
            })
        }
    }

    override suspend fun main() {
        val galleySeen = GalleySeen(this)

        setContentSeen(galleySeen)

        chooseType = intent.getStringExtra(CHOOSE_MODE) ?: ""

        if (chooseType.isNotEmpty()) galleySeen.showActionBtn()

        suspend fun initPager() {
            galleySeen.initPager(arrayListOf<Fragment>().apply {
                add(GalleyFragment(false, userConfig.lockGalley.isNotEmpty()).also {
                    galleySeen.setMailer(frag1 = it.fragMailer)
                    it.iGalleyFragment = this@GalleyActivity.iGalleyFragment
                })
                add(GalleyFragment(true, userConfig.lockGalley.isNotEmpty()).also {
                    galleySeen.setMailer(frag2 = it.fragMailer)
                    it.iGalleyFragment = this@GalleyActivity.iGalleyFragment
                })
            }, chooseType)
        }

        initPager()
        Medias.mediaLive.observe(this) {
            Log.d(TAG, "main: $it")
            if (it == Medias.GALLEY_UPDATED) {
                galleySeen.offer(GalleySeen.Touch.Init)
            }
        }
        chooseData.observe(this) {
            if (it.size > 0) {
                galleySeen.onAction()
            }
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                galleySeen.viewEvent.onReceive {
                    when (it) {
                        GalleySeen.Touch.Init -> initPager()
                        GalleySeen.Touch.Finish -> finish()
                        GalleySeen.Touch.MOVE_TO -> galleySeen.moveTo()
                        GalleySeen.Touch.RENAME -> galleySeen.rename()
                        GalleySeen.Touch.ADD_CATE -> galleySeen.addCate()
                        GalleySeen.Touch.SELECT_ALL -> galleySeen.selectAll()
                        GalleySeen.Touch.DELETE -> galleySeen.delete()
                        GalleySeen.Touch.IntoBOX -> {
                            IntentDataHolder.put((chooseData() ?: galleySeen.getChooseGalley()))
                            startActivity(PictureBoxActivity::class.intent)
                        }
                        GalleySeen.Touch.ConfirmChoose -> {
                            chooseData.value?.let { list ->
                                if (list.size > 0) {
                                    setResult(RESULT_OK, Intent().apply {
                                        putExtra(URI, list[0].uri.toUriJson())
                                        putExtra(GALLEY_DATA, list[0].toJson())
                                    })
                                }
                            }
                            finish()
                        }
                        GalleySeen.Touch.ShowPop -> galleySeen.showPop(
                            (chooseData()?.size ?: 0) > 0
                        )
                    }
                }
            }
        }
    }

    private fun GalleySeen.rename() = chooseData.value?.get(0)?.let {
        val mimeType = it.name.getFileMimeType()
        TitleDialog(
            context,
            getString(R.string.rename),
            it.name.replace(mimeType, "")
        ) { name ->
            renameMedia(name + mimeType, it.uri) { result ->
                if (result) {
                    it.name = name + mimeType
                    toast(getString(R.string.success))
                } else toast(getString(R.string.not_supported))
            }
        }
    }


    private suspend fun GalleySeen.delete() = withContext(Dispatchers.IO) {
        chooseData.value?.get(0)?.let {
            deleteMedia(it.uri) { result ->
                if (result) {
                    DataBaseDAOHelper.deleteSignedMedia(it)
                    deleteMedia(it)
                    toast(getString(R.string.success))
                } else toast(getString(R.string.not_supported))
            }
        }
    }

    private fun GalleySeen.addCate() {
        CateDialog(context, {
            TitleDialog(context, getString(R.string.addCate), "") { re ->
                if (re.isEmpty()) {
                    toast("请输入内容")
                    return@TitleDialog
                }
                addCate(re)
            }
        }) {
            launch {
                startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    GalleyActivity::class.intent.also { intent ->
                        intent.putExtra(
                            CHOOSE_MODE,
                            GalleySeen.CHOOSE_PHOTO
                        )
                    }
                )?.let { result ->
                    val uri = result.data?.getStringExtra(URI)
                    if (uri != null) {
                        addCate(uri)
                    } else showFailedToast()
                }
            }
        }
    }

    private fun addCate(cate: String) {
        chooseData.value?.let { list ->
            list.forEach { gm ->
                gm.also { g ->
                    if (g.cate == null) g.cate = mutableListOf()
                    (g.cate as MutableList).add(cate)
                }
            }
            DataBaseDAOHelper.updateMediaMulti(list)
        }
    }


    private suspend fun GalleySeen.moveTo() {
        val pop = ColorfulPopWindow(this@GalleyActivity)
        pop.startListPopup(
            getBar(),
            withContext(Dispatchers.IO) {
                val list = mutableListOf<String>()
                DataBaseDAOHelper.getALLGalleyBucket(rightMailer != 0)
                    ?.forEach {
                        list.add(it.type)
                    }
                list
            }) { re ->
            if (re != null) {
                chooseData.value?.let { list ->
                    list.forEach {
                        if (it.type == null) it.type = mutableListOf()
                        if (it.type?.contains(re) == false)
                            (it.type as MutableList<String>).add(re)
                        else toast("${it.name}已存在${it.type}中")
                    }
                    DataBaseDAOHelper.updateMediaMulti(list)
                    addBucket(re, list)
                }
            } else toast(getString(R.string.none))
            pop.dismiss()
        }
    }

    private fun chooseData() = chooseData.value
}