package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.AddBucketLayoutBinding

class AddBucketSeen(context: Context) : Seen<AddBucketSeen.Event>(context) {

    enum class Event{
        ChooseIcon,
        Confirm,
        Finished
    }

    val binding = AddBucketLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root


    init {
        binding.self = this
    }

    fun offer(event: Event){
        viewEvent.offer(event)
    }
}