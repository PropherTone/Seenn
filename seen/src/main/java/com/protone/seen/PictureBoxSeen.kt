package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.PictureBoxLayoutBinding

class PictureBoxSeen(context: Context) : Seen<PictureBoxSeen.PictureBox>(context) {

    enum class PictureBox{
        SelectPicList
    }

    val binding = PictureBoxLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root

    init {
        binding.self = this
    }

    fun offer(msg: PictureBox) {
        viewEvent.offer(msg)
    }
}