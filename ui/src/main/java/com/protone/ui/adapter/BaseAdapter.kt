package com.protone.ui.adapter

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
    private val handleEvent: Boolean = false
) : RecyclerView.Adapter<BaseAdapter.Holder<B>>(),
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    class Holder<B : ViewDataBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    private val _adapterFlow = MutableSharedFlow<T>()
    private val adapterFlow get() = _adapterFlow

    open suspend fun onEventIO(data: T) {}

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (handleEvent) launch(Dispatchers.IO) {
            adapterFlow.buffer().collect {
                onEventIO(it)
            }
        }
    }

    protected fun emit(value: T) {
        launch {
            adapterFlow.emit(value)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cancel()
    }
}