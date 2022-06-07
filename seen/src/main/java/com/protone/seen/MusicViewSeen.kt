package com.protone.seen

import android.content.Context
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.Music
import com.protone.seen.adapter.TransparentPlayListAdapter
import com.protone.seen.databinding.MusicViewLayoutBinding

class MusicViewSeen(context: Context) : Seen<MusicViewSeen.MusicEvent>(context) {

    enum class MusicEvent {
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
        binding.self = this
    }

    fun initPlayList(playList: MutableList<Music>) {
        binding.playList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TransparentPlayListAdapter(context, playList)
        }
    }

    fun showPop() {
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup?)
        binding.pop.isGone = !binding.pop.isGone
    }

}