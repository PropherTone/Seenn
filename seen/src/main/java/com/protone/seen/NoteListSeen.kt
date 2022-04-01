package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.NoteListLayoutBinding

class NoteListSeen(context: Context) : Seen<NoteListSeen.NoteListEvent>(context) {

    enum class NoteListEvent{

    }

    private val binding = NoteListLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root

    override fun offer(event: NoteListEvent) {

    }
}