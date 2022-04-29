package com.protone.seen

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.Music
import com.protone.seen.adapter.MusicListAdapter
import com.protone.seen.customView.DragRefreshView
import com.protone.seen.databinding.UserConfigLayoutBinding

class UserConfigSeen(context: Context) : Seen<UserConfigSeen.UserEvent>(context) {

    enum class UserEvent {

    }

    private val binding =
        UserConfigLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View get() = binding.root

    override fun getToolBar(): View? = null

    override fun offer(event: UserEvent) {
        viewEvent.offer(event)
    }

    init {
        binding.self = this
        binding.drag.overScrollListener = object : DragRefreshView.OverScrolled{
            override fun onTop(drag: Float) {
                binding.cover.setData(drag)
            }

            override fun onBot(drag: Float) {
                binding.cover.setData(drag)
            }

            override fun onStop(isTop: Boolean) {

            }

        }
    }

    fun initList(list: MutableList<Music>){
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MusicListAdapter(context).apply {
                this.musicList = list
            }
        }
    }
}