package com.protone.seen.adapter

import android.content.Context
import androidx.databinding.ViewDataBinding

abstract class SelectListAdapter<V : ViewDataBinding, T>(context: Context) :
    BaseAdapter<V>(context) {

    val selectList = mutableListOf<T>()
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

    private fun clearSelected() {
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
}