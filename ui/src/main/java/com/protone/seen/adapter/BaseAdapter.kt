package com.protone.seen.adapter

import android.content.Context
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class BaseAdapter<B : ViewDataBinding>(
    val context: Context
) : RecyclerView.Adapter<BaseAdapter.Holder<B>>(),CoroutineScope by CoroutineScope(Dispatchers.Main) {

    class Holder<B : ViewDataBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cancel()
    }
}