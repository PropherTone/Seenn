package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.NoteEditLayoutBinding

class NoteEditSeen(context: Context) : Seen<NoteEditSeen.NoteEditEvent>(context) {

    enum class NoteEditEvent{

    }

    private val binding = NoteEditLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root
}