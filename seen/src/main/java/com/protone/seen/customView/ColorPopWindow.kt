package com.protone.seen.customView

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.R
import com.protone.seen.customView.colorPicker.MyColorPicker
import com.protone.seen.databinding.ColorPopLayoutBinding
import com.protone.seen.databinding.NumberPickerPopLayoutBinding
import java.lang.ref.WeakReference

class ColorPopWindow(context: Context) : PopupWindow(context) {

    var weakContext: WeakReference<Context> = WeakReference(context)

    inline fun startColorPickerPopup(anchor: View, crossinline onCall: (Int) -> Unit) =
        weakContext.get()?.let { context ->
            val binder = ColorPopLayoutBinding.inflate(context.layoutInflater, context.root, false)
            startPopup(context, binder.root, anchor) {
                (contentView.findViewById(R.id.pop_Color_picker) as MyColorPicker).onColorChangeListener {
                    onCall(
                        it
                    )
                }
            }
        }

    inline fun startNumberPickerPopup(anchor: View, crossinline onCall: (Int) -> Unit) =
        weakContext.get()?.let { context ->
            val binder =
                NumberPickerPopLayoutBinding.inflate(context.layoutInflater, context.root, false)
            startPopup(context, binder.root, anchor) {
                (contentView.findViewById(R.id.number_picker) as NumberPicker).setOnValueChangedListener { _, _, newVal ->
                    onCall.invoke(newVal)
                }
            }
        }

    inline fun startPopup(context: Context, view: View, anchor: View, func: () -> Unit) {
        contentView = view
        width = anchor.measuredWidth
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        this.isOutsideTouchable = true
        setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.background_ripple_transparent_white
            )
        )
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        showAtLocation(
            anchor,
            Gravity.NO_GRAVITY,
            ((location[0] - anchor.measuredWidth / 2) - width / 2),
            location[1] - anchor.height
        )
        func.invoke()
    }

}