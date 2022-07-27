package com.protone.seen.adapter

import android.content.Context
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class BaseAdapter<B : ViewDataBinding, T>(
    val context: Context,
    protected val handleEvent: Boolean = false
) : RecyclerView.Adapter<BaseAdapter.Holder<B>>(),
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    class Holder<B : ViewDataBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    private val _adapterFlow = MutableSharedFlow<T>()
    val adapterFlow get() = _adapterFlow

    open suspend fun onEventIO(data: T) {}

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (handleEvent) launch(Dispatchers.IO) {
            adapterFlow.buffer().collect {
                onEventIO(it)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cancel()
    }
}