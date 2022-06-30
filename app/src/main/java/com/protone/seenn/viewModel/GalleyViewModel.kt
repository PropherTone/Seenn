package com.protone.seenn.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.tabs.TabLayout
import com.protone.api.context.Global
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.media.FragMailer
import com.protone.mediamodle.media.IGalleyFragment
import com.protone.seen.R

class GalleyViewModel : ViewModel(), TabLayout.OnTabSelectedListener {
    companion object {
        @JvmStatic
        val CUSTOM = "CustomChoose"

        @JvmStatic
        val CHOOSE_MODE = "ChooseData"

        @JvmStatic
        val URI = "Uri"

        @JvmStatic
        val GALLEY_DATA = "GalleyData"

        @JvmStatic
        val CHOOSE_PHOTO = "PHOTO"

        @JvmStatic
        val CHOOSE_VIDEO = "VIDEO"
    }

    var chooseType = ""

    var rightMailer = 0

    private val mailers = arrayOfNulls<FragMailer>(2)

    val chooseData: MutableLiveData<MutableList<GalleyMedia>> =
        MutableLiveData<MutableList<GalleyMedia>>()

    var startActivity: ((GalleyMedia, String) -> Unit)? = null

    val iGalleyFragment = object : IGalleyFragment {
        override fun select(galleyMedia: MutableList<GalleyMedia>) {
            chooseData.postValue(galleyMedia)
        }

        override fun openView(galleyMedia: GalleyMedia, galley: String) {
            startActivity?.invoke(galleyMedia, galley)
        }
    }

    fun chooseData() = chooseData.value

    fun setMailer(frag1: FragMailer? = null, frag2: FragMailer? = null) {
        if (frag1 != null) {
            mailers[0] = frag1
        } else if (frag2 != null) {
            mailers[1] = frag2
        }
    }

    fun onAction() {
        mailers[rightMailer]?.onActionBtn()
    }

    fun getChooseGalley(): MutableList<GalleyMedia>? {
        return mailers[rightMailer]?.getChooseGalley()
    }

    fun deleteMedia(media: GalleyMedia) {
        mailers[rightMailer]?.deleteMedia(media)
    }

    fun addBucket(name: String, list: MutableList<GalleyMedia>) {
        mailers[rightMailer]?.addBucket(name, list)
    }

    fun selectAll() {
        mailers[rightMailer]?.selectAll()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.text) {
            Global.app.getString(R.string.photo) -> rightMailer = 0
            Global.app.getString(R.string.video) -> rightMailer = 1
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
    override fun onTabReselected(tab: TabLayout.Tab?) = Unit
}