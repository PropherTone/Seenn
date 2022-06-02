package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.MusicViewLayoutBinding

class MusicViewSeen(context: Context) : Seen<MusicViewSeen.MusicEvent>(context) {

    enum class MusicEvent{
        Finish
    }

    private val binding =
        MusicViewLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View = binding.root

    override fun getToolBar(): View = binding.toolBar

    val controller = binding.musicPlayer

    override fun offer(event: MusicEvent) {
        viewEvent.offer(event)
    }

    init {
        setSettleToolBar()
    }

}