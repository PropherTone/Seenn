package com.protone.seen.adapter

import android.content.Context
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<B : ViewDataBinding>(
    val context: Context
) : RecyclerView.Adapter<BaseAdapter.Holder<B>>() {

    class Holder<B : ViewDataBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)
}