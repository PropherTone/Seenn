package com.protone.seen

import android.content.Context
import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.toMediaBitmapByteArray
import com.protone.seen.databinding.AddBucketLayoutBinding

class AddBucketSeen(context: Context) : Seen<AddBucketSeen.Event>(context) {

    enum class Event {
        ChooseIcon,
        Confirm,
        Finished
    }

    val binding = AddBucketLayoutBinding.inflate(context.layoutInflater, context.root, true)

    val name: String
        get() = binding.musicBucketEnterName.text.toString()

    val detail: String
        get() = binding.musicBucketEnterDetail.text.toString()

    var uri: Uri? = null
        set(value) {
            loadIcon(value?.toMediaBitmapByteArray())
            field = value
        }

    override val viewRoot: View
        get() = binding.root


    init {
        binding.self = this
    }

    override fun offer(event: Event) {
        viewEvent.offer(event)
    }

    private fun loadIcon(model: ByteArray?) = binding.musicBucketIcon.apply {
        Glide.with(context).load(model).into(this)
    }

}