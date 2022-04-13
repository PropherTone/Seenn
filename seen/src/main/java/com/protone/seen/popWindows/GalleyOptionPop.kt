package com.protone.seen.popWindows

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import com.protone.api.Config
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.R
import com.protone.seen.databinding.GalleyOptionPopBinding

class GalleyOptionPop(val context: Context,val view: View) {

    private val popupWindow = PopupWindow()

    init {
        popupWindow.apply {
            contentView = view
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            width = ViewGroup.LayoutParams.MATCH_PARENT
            isOutsideTouchable = true
            isFocusable = true
            elevation = 10f
        }
    }

    fun showPop(view: View){
        popupWindow.showAsDropDown(view)
    }
}