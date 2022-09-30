package com.protone.ui.popWindows

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow

class GalleryOptionPop(val context: Context, val view: View) : PopupWindow() {

    init {
        contentView = view
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.MATCH_PARENT
        isOutsideTouchable = true
        isFocusable = true
        elevation = 10f
    }

    fun showPop(view: View) {
        showAsDropDown(view)
    }
}