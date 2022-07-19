package com.protone.seen.adapter

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseCheckListAdapter<B : ViewDataBinding, T>(val dataList: List<T>) :
    RecyclerView.Adapter<BaseCheckListAdapter.Holder<B>>(),
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    class Holder<B : ViewDataBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int {
        return dataList.size
    }

    private val select = arrayListOf<T>()

    override fun onBindViewHolder(holder: Holder<B>, position: Int) {
        setSelect(select.contains(dataList[position]))
    }

    fun checkSelect(item: T) {
        if (select.contains(item)) {
            unselectItem(item)
            setSelect(false)
        } else {
            clearSelect()
            selectItem(item)
            setSelect(true)
        }
    }

    private fun selectItem(item: T) {
        select.add(item)
    }

    private fun unselectItem(item: T) {
        select.remove(item)
    }

    private fun clearSelect() {
        if (dataList.isNotEmpty() && select.isNotEmpty()) {
            dataList.indexOf(select[0]).apply {
                if (this != -1) {
                    notifyItemChanged(this)
                }
            }
            select.clear()
        }
    }

    abstract val setOnCheck: (Boolean) -> Unit

    private fun setSelect(check: Boolean) {
        setOnCheck(check)
    }

    protected fun clearAllSelect() {
        launch {
            val indexList = arrayListOf<Int>().apply {
                for (t in select) {
                    val index = dataList.indexOf(t)
                    select.remove(t)
                    add(index)
                } }
            withContext(Dispatchers.Main){
                for (i in indexList) {
                    notifyItemChanged(i)
                }
            }
        }
    }

}