package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.mediamodle.Galley
import com.protone.seen.adapter.AddMusicListAdapter
import com.protone.seen.databinding.AddMusicBucketLayoutBinding

class PickMusicActivity(context: Context) : Seen<PickMusicActivity.Event>(context) {

    enum class Event {
        Finished,
        Confirm
    }

    companion object {
        const val BUCKET_NAME = "BUCKET"
        const val MODE = "MODE"

        const val ADD_BUCKET = "ADD"
        const val PICK_MUSIC = "PICK"
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

    override fun getToolBar(): View = binding.view

    init {
        setSettleToolBar()
        binding.self = this
    }

    fun initSeen(bucket: String, mode: String) {
        binding.addMBConfirm.also {
            it.isGone = mode == ADD_BUCKET
            binding.addMBLeave.isGone = !it.isGone
        }
        binding.addMBList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AddMusicListAdapter(context, bucket, mode).apply {
                musicList = Galley.music
            }
        }
    }

    override fun offer(event: Event) {
        viewEvent.offer(event)
    }

    fun getSelectList() = binding.addMBList.adapter.let {
        if (it is AddMusicListAdapter) {
            it.selectList
        }else null
    }
}