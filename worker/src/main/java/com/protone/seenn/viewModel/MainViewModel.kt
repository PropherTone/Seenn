package com.protone.seenn.viewModel

import com.protone.api.context.SApplication
import com.protone.seenn.R

class MainViewModel : BaseViewModel() {
    var btnY = 0f
    val btnH = SApplication.app.resources.getDimensionPixelSize(R.dimen.action_icon_p)
}