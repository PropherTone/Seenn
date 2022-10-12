package com.protone.ui.popWindows

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.protone.api.baseType.toInt
import com.protone.api.context.newLayoutInflater
import com.protone.api.context.root
import com.protone.api.spans.SpanStates
import com.protone.ui.R
import com.protone.ui.adapter.CheckListAdapter
import com.protone.ui.databinding.*
import java.lang.ref.WeakReference

class ColorfulPopWindow(context: Context) : PopupWindow(context) {

    var weakContext: WeakReference<Context> = WeakReference(context)

    inline fun startColorPickerPopup(
        anchor: View,
        isUpToBot: Boolean = false,
        crossinline onCall: (Int) -> Unit
    ) = weakContext.get()?.let { context ->
        val binding =
            ColorPopLayoutBinding.inflate(context.newLayoutInflater, context.root, false)
        binding.popColorPicker.onColorChangeListener { onCall(it) }
        startPopup(context, binding.root, anchor, isUpToBot)
    }

    inline fun startNumberPickerPopup(
        anchor: View,
        number: Int,
        isUpToBot: Boolean = false,
        crossinline onCall: (Int) -> Unit
    ) = weakContext.get()?.let { context ->
        val binding =
            NumberPickerPopLayoutBinding.inflate(context.newLayoutInflater, context.root, false)
        binding.numberPicker.apply {
            maxValue = 999
            minValue = 1
            value = number
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            setOnValueChangedListener { _, _, newVal -> onCall.invoke(newVal) }
        }
        startPopup(context, binding.root, anchor, isUpToBot)
    }

    inline fun startListPopup(
        title: String = "",
        anchor: View,
        dataList: MutableList<String>,
        isUpToBot: Boolean = false,
        crossinline onCall: (String?) -> Unit
    ) = weakContext.get()?.let { context ->
        val binding =
            ListPopWindowsLayoutBinding.inflate(context.newLayoutInflater, context.root, false)
        binding.listTitle.text = title
        binding.listList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CheckListAdapter(context, dataList)
        }
        binding.listConfirm.setOnClickListener {
            binding.listList.adapter.let {
                if (it is CheckListAdapter)
                    onCall.invoke(if (it.selectList.size > 0) it.selectList[0] else null)
            }
        }
        binding.listCancel.setOnClickListener { onCall.invoke(null) }
        startPopup(context, binding.root, anchor, isUpToBot)
    }

    inline fun startBulletSpanSettingPop(
        anchor: View,
        crossinline onResult: (Int?, Any?, Int?) -> Unit
    ) = weakContext.get()?.let { context ->
        val binding =
            BulletSpanOptionLayoutBinding.inflate(
                context.newLayoutInflater,
                context.root,
                false
            ).apply {
                var color: Int = Color.BLACK
                colorPicker.onColorChangeListener {
                    pickColor.setBackgroundColor(it)
                    color = it
                }
                pickColor.setOnClickListener {
                    TransitionManager.beginDelayedTransition(root as ViewGroup)
                    colorPicker.isGone = !colorPicker.isGone
                }
                more.setOnClickListener {
                    TransitionManager.beginDelayedTransition(root as ViewGroup)
                    if (pickColor.isGone) {
                        pickColor.isGone = false
                    } else {
                        radiusNotice.isGone = false
                        radius.isGone = false
                    }
                }
                confirm.setOnClickListener {
                    onResult.invoke(gapWidth.text.toInt(), color, radius.text.toInt())
                }
            }
        startPopup(context, binding.root, anchor, false)
    }

    inline fun startQuoteSpanSettingPop(
        anchor: View,
        crossinline onResult: (Any?, Int?, Int?) -> Unit
    ) = weakContext.get()?.let { context ->
        val binding =
            QuoteSpanOptionalLayoutBinding.inflate(
                context.newLayoutInflater,
                context.root,
                false
            ).apply {
                var color: Int = Color.BLACK
                colorPicker.onColorChangeListener {
                    pickColor.setBackgroundColor(it)
                    color = it
                }
                pickColor.setOnClickListener {
                    TransitionManager.beginDelayedTransition(root as ViewGroup)
                    colorPicker.isGone = !colorPicker.isGone
                }
                more.setOnClickListener {
                    TransitionManager.beginDelayedTransition(root as ViewGroup)
                    if (stripeWidth.isGone) {
                        stripeWidth.isGone = false
                        stripeWidthNotice.isGone = false
                    } else {
                        gapWidth.isGone = false
                        gapWidthNotice.isGone = false
                    }
                }
                confirm.setOnClickListener {
                    onResult.invoke(color, stripeWidth.text.toInt(), gapWidth.text.toInt())
                }
            }
        startPopup(context, binding.root, anchor, false)
    }

    inline fun startParagraphSpanSettingPop(
        anchor: View,
        crossinline onResult: (SpanStates.SpanAlignment) -> Unit
    ) = weakContext.get()?.let { context ->
        val binding = ParagraphSpanOptionalLayoutBinding.inflate(
            context.newLayoutInflater,
            context.root,
            false
        ).apply {
            alignCenter.setOnClickListener {
                onResult.invoke(SpanStates.SpanAlignment.ALIGN_CENTER)
            }
            alignLeft.setOnClickListener {
                onResult.invoke(SpanStates.SpanAlignment.ALIGN_NORMAL)
            }
            alignRight.setOnClickListener {
                onResult.invoke(SpanStates.SpanAlignment.ALIGN_OPPOSITE)
            }
            firstAlign.setOnClickListener {
                onResult.invoke(SpanStates.SpanAlignment.FIRST_LINE_ALIGN)
            }
        }
        startPopup(context, binding.root, anchor, false)
    }

    inline fun startPopup(
        context: Context,
        view: View,
        anchor: View,
        isUpToBot: Boolean,
        func: () -> Unit = {}
    ) {
        contentView = view
        width = anchor.measuredWidth
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        this.isOutsideTouchable = true
        this.isFocusable = true
        this.animationStyle =
            if (isUpToBot) R.style.PopAnimationUpToBot else R.style.PopAnimationBotToUp
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