package com.protone.seen

import android.content.Context
import android.view.DragEvent
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.NoteSyncLayoutBinding

class NoteSyncSeen(context: Context) : Seen<NoteSyncSeen.NoteSync>(context) {

    enum class NoteSync{
        Send,
        Receive
    }

    private val binding = NoteSyncLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root

    init {
        binding.self = this
    }

    override fun offer(event : NoteSync){
        viewEvent.offer(event)
    }
}