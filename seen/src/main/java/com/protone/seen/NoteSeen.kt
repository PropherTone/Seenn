package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.NoteLayoutBinding

class NoteSeen(context: Context) : Seen<NoteSeen.NoteEvent>(context){

    enum class NoteEvent{

    }

    val binding = NoteLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root
}