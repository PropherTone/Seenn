package com.protone.seenn.viewModel

import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayout
import com.protone.api.baseType.getString
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.R
import com.protone.seenn.media.FragMailer
import com.protone.seenn.media.IGalleyFragment

class GalleyViewModel : BaseViewModel(), TabLayout.OnTabSelectedListener {
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

    var onTransaction = false

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

    fun onUpdate(updateList: MutableList<GalleyMedia>) {
        mailers[rightMailer]?.onGalleyUpdate(updateList)
    }

    fun addBucket(name: String, list: MutableList<GalleyMedia>) {
        mailers[rightMailer]?.addBucket(name, list)
    }

    fun selectAll() {
        mailers[rightMailer]?.selectAll()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.text) {
            R.string.photo.getString() -> rightMailer = 0
            R.string.video.getString() -> rightMailer = 1
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
    override fun onTabReselected(tab: TabLayout.Tab?) = Unit
}