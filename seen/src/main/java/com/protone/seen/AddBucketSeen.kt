package com.protone.seen

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.protone.api.TAG
import com.protone.api.context.layoutInflater
import com.protone.api.context.marginBottom
import com.protone.api.context.root
import com.protone.api.context.setSoftInputStatuesListener
import com.protone.api.toMediaBitmapByteArray
import com.protone.seen.databinding.AddBucketLayoutBinding

class AddBucketSeen(context: Context) : Seen<AddBucketSeen.Event>(context) {

    enum class Event {
        ChooseIcon,
        Confirm,
        Finished
    }

    val binding = AddBucketLayoutBinding.inflate(context.layoutInflater, context.root, true)

    var name: String
        set(value) {
            binding.musicBucketEnterName.setText(value)
        }
        get() = binding.musicBucketEnterName.text.toString()

    var detail: String
        set(value) {
            binding.musicBucketEnterDetail.setText(value)
        }
        get() = binding.musicBucketEnterDetail.text.toString()

    var uri: Uri? = null
        set(value) {
            Log.d(TAG, "$value: ")
            loadIcon(value?.toMediaBitmapByteArray())
            field = value
        }

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.musicBucketParent

    init {
        initToolBar()
        binding.self = this
        if (context is Activity) context.setSoftInputStatuesListener { i, b ->
            if (b) {
                binding.root.marginBottom(i)
            } else {
                binding.root.marginBottom(0)
            }
        }
    }

    override fun offer(event: Event) {
        viewEvent.trySend(event)
    }

    fun loadIcon(model: ByteArray?) = binding.musicBucketIcon.apply {
        Glide.with(this@AddBucketSeen.context).load(model).into(this)
    }


}