package com.protone.seen

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.adapter.AddMusicListAdapter
import com.protone.seen.databinding.AddMusicBucketLayoutBinding

class AddMusic2BucketSeen(context: Context) : Seen<AddMusic2BucketSeen.Event>(context) {

    enum class Event {
        Finished
    }

    private val binding by lazy {
        AddMusicBucketLayoutBinding.inflate(
            context.layoutInflater,
            context.root,
            false
        )
    }

    override val viewRoot: View
        get() = binding.root

    init {
        binding.self = this
    }

    fun initSeen() {
        binding.addMBList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AddMusicListAdapter(context)
        }
    }

    override fun offer(event: Event) {
        viewEvent.offer(event)
    }
}