package com.protone.seen.customView

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.R
import com.protone.seen.customView.colorPicker.MyColorPicker
import com.protone.seen.databinding.ColorPopLayoutBinding
import java.lang.ref.WeakReference

class ColorPopWindow(context: Context) {

    var weakContext: WeakReference<Context> = WeakReference(context)

   inline fun startPopWindow(view: View,crossinline onCall: (Int) -> Unit) = weakContext.get()?.let { context ->
        PopupWindow(context).apply {
            val binder = ColorPopLayoutBinding.inflate(context.layoutInflater, context.root, false)
            contentView = binder.root
            width = view.measuredWidth
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            this.isOutsideTouchable = true
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.background_ripple_transparent_white
                )
            )
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            showAtLocation(
                view,
                Gravity.NO_GRAVITY,
                ((location[0] - view.measuredWidth / 2) - width / 2),
                location[1] - view.height
            )
            (contentView.findViewById(R.id.pop_Color_picker) as MyColorPicker).onColorChangeListener { onCall(it) }
        }
    }

}