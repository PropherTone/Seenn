package com.protone.seenn.viewModel

import androidx.lifecycle.viewModelScope
import com.google.android.material.tabs.TabLayout
import com.protone.api.baseType.getString
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

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

    private val mailers = arrayOfNulls<MutableSharedFlow<GalleyFragmentViewModel.FragEvent>>(2)

    var chooseData: MutableList<GalleyMedia>? = null

    fun setMailer(
        frag1: MutableSharedFlow<GalleyFragmentViewModel.FragEvent>? = null,
        frag2: MutableSharedFlow<GalleyFragmentViewModel.FragEvent>? = null
    ) {
        viewModelScope.launch {
            if (frag1 != null) {
                mailers[0] = frag1
            } else if (frag2 != null) {
                mailers[1] = frag2
            }
        }
    }

    fun onAction() {
        viewModelScope.launch {
            mailers[rightMailer]?.emit(GalleyFragmentViewModel.FragEvent.OnActionBtn)
        }
    }

    fun intoBox() {
        viewModelScope.launch {
            mailers[rightMailer]?.emit(GalleyFragmentViewModel.FragEvent.IntoBox)
        }
    }

    fun deleteMedia(media: GalleyMedia) {
        viewModelScope.launch {
            mailers[rightMailer]?.emit(GalleyFragmentViewModel.FragEvent.DeleteMedia(media))
        }
    }

    fun onUpdate(media: GalleyMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            mailers[rightMailer]?.emit(GalleyFragmentViewModel.FragEvent.OnGalleyUpdate(media))
        }
    }

    fun addBucket(name: String, list: MutableList<GalleyMedia>) {
        viewModelScope.launch {
            mailers[rightMailer]?.emit(GalleyFragmentViewModel.FragEvent.AddBucket(name, list))
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            mailers[rightMailer]?.emit(GalleyFragmentViewModel.FragEvent.SelectAll)
        }
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