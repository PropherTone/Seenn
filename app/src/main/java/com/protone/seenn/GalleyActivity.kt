package com.protone.seenn

import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.intent
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.mediamodle.media.IGalleyFragment
import com.protone.seen.GalleySeen
import com.protone.seenn.fragment.GalleyFragment
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

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
                        GalleySeen.Touch.RENAME -> rename()
                        GalleySeen.Touch.ADD_CATE -> addCate()
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

    private fun rename() = chooseData.value?.get(0)?.let {
        rename(it, this)
    }

    private fun GalleySeen.delete() = chooseData.value?.get(0)?.let {
        delete(it, this) { re ->
            deleteMedia(re)
        }
    }

    private fun addCate() = chooseData.value?.let { list ->
        addCate(list)
    }

    private suspend fun GalleySeen.moveTo() = chooseData.value?.let {
        moveTo(getBar(), rightMailer != 0, it) { target, list ->
            addBucket(target, list)
        }
    }

    private fun chooseData() = chooseData.value
}