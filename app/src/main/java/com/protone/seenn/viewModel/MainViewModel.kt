package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.protone.api.context.SApplication

class MainViewModel : ViewModel() {
    var btnY = 0f
    val btnH = SApplication.app.resources.getDimensionPixelSize(com.protone.seen.R.dimen.action_icon_p)
}