package com.protone.seen.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.protone.api.animation.AnimationHelper
import com.protone.seen.R

abstract class SelectListAdapter<V : ViewDataBinding, T>(context: Context) :
    BaseAdapter<V>(context) {

    var selectList = mutableListOf<T>()
    var multiChoose = false

    open fun checkSelect(holder: Holder<V>, item: T) {
        if (selectList.contains(item)) {
            selectList.remove(item)
            setSelect(holder, false)
        } else {
            if (!multiChoose) clearSelected()
            selectList.add(item)
            setSelect(holder, true)
        }
    }

    abstract val select: (holder: Holder<V>, isSelect: Boolean) -> Unit
    abstract fun itemIndex(path: T): Int

    fun setSelect(holder: Holder<V>, state: Boolean) = select(holder, state)

    fun clearSelected() {
        if (selectList.size > 0) {
            val itemIndex = itemIndex(selectList[0])
            selectList.clear()
            if (itemIndex != -1) {
                notifyItemChanged(itemIndex)
            }
        }
    }

    fun clearAllSelected() {
        selectList.forEach {
            val itemIndex = itemIndex(it)
            if (itemIndex != -1) notifyItemChanged(itemIndex)
        }
        selectList.clear()
    }

    fun clickAnimation(
        pressed: Boolean,
        background: View?,
        container: ViewGroup?,
        visible: View?,
        vararg texts: TextView,
        dispatch: Boolean = true
    ) {
        background?.setBackgroundColor(
            ContextCompat.getColor(
                context,
                if (pressed) R.color.blue_1 else R.color.white
            )
        )
        visible?.visibility = if (pressed) View.VISIBLE else View.GONE
        texts.forEach {
            it.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (pressed) R.color.white else R.color.black
                )
            )
        }
        itemClickChange(
            if (pressed) R.color.blue_1 else R.color.white,
            if (pressed) R.color.white else R.color.black,
            background, container, texts, pressed && dispatch
        )
    }

    fun itemClickChange(
        backgroundColor: Int,
        textsColor: Int,
        background: View?,
        container: ViewGroup?,
        texts: Array<out TextView>,
        pressed: Boolean,
    ) {
        background?.setBackgroundColor(
            ContextCompat.getColor(
                context,
                backgroundColor
            )
        )
        texts.forEach {
            it.setTextColor(
                ContextCompat.getColor(
                    context,
                    textsColor
                )
            )
        }
        if (container != null && pressed) {
            startAnimation(container)
        }
    }

    private fun startAnimation(target: ViewGroup) {
        AnimationHelper.apply {
            val x = scaleX(target, 0.96f, duration = 50)
            val y = scaleY(target, 0.96f, duration = 50)
            val x1 = scaleX(target, 1f, duration = 360)
            val y1 = scaleY(target, 1f, duration = 360)
            animatorSet(x, y, play = true, doOnEnd = {
                animatorSet(x1, y1, play = true)
            })
        }
    }
}