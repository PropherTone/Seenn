package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.google.android.material.tabs.TabLayout
import com.protone.api.baseType.getString
import com.protone.api.entity.GalleyMedia
import com.protone.worker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GalleyViewModel : BaseViewModel(), TabLayout.OnTabSelectedListener {

    companion object {
        const val CHOOSE_MODE = "ChooseData"
        const val URI = "Uri"
        const val GALLEY_DATA = "GalleyData"
        const val CHOOSE_PHOTO = "PHOTO"
        const val CHOOSE_VIDEO = "VIDEO"
    }

    var chooseData: MutableList<GalleyMedia>? = null
        private set

    private var rightMailer = 0

    private val mailers = arrayOfNulls<MutableSharedFlow<GalleyFragmentViewModel.FragEvent>>(2)

    fun setMailer(
        frag1: MutableSharedFlow<GalleyFragmentViewModel.FragEvent>? = null,
        frag2: MutableSharedFlow<GalleyFragmentViewModel.FragEvent>? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            if (frag1 != null) {
                mailers[0] = frag1
                mailers[0]?.collect(onAction())
            } else if (frag2 != null) {
                mailers[1] = frag2
                mailers[1]?.collect(onAction())
            }
        }
    }

    fun intoBox() {
        viewModelScope.launch {
            getCurrentMailer()?.emit(GalleyFragmentViewModel.FragEvent.IntoBox)
        }
    }

    fun addBucket(name: String, list: MutableList<GalleyMedia>) {
        viewModelScope.launch {
            getCurrentMailer()?.emit(GalleyFragmentViewModel.FragEvent.AddBucket(name, list))
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            getCurrentMailer()?.emit(GalleyFragmentViewModel.FragEvent.SelectAll)
        }
    }

    private fun onAction(): suspend (value: GalleyFragmentViewModel.FragEvent) -> Unit = {
        when (it) {
            is GalleyFragmentViewModel.FragEvent.OnSelect -> {
                if (chooseData == null) chooseData = it.galleyMedia
                if (it.galleyMedia.size > 0) getCurrentMailer()?.emit(GalleyFragmentViewModel.FragEvent.OnActionBtn)
            }
            else -> Unit
        }
    }

    private fun getCurrentMailer() = mailers[rightMailer]

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.text) {
            R.string.photo.getString() -> rightMailer = 0
            R.string.video.getString() -> rightMailer = 1
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
    override fun onTabReselected(tab: TabLayout.Tab?) = Unit
}