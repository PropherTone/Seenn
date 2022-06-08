package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Medias
import com.protone.seen.adapter.AddMusicListAdapter
import com.protone.seen.databinding.AddMusicBucketLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PickMusicSeen(context: Context) : Seen<PickMusicSeen.Event>(context) {

    enum class Event {
        Finished,
        Confirm,
        Query
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

    fun getQueryInput() = binding.addMBSearch

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
                musicList = Medias.music
            }
        }
    }

    override fun offer(event: Event) {
        viewEvent.offer(event)
    }

    suspend fun refreshList(list: MutableList<Music>) = withContext(Dispatchers.Main) {
        binding.addMBList.adapter.let {
            if (it is AddMusicListAdapter) {
                it.noticeDataUpdate(list)
            }
        }
    }

    fun getList(): MutableList<Music>? = binding.addMBList.adapter.let {
        if (it is AddMusicListAdapter) {
            return it.musicList
        } else null
    }

    fun getSelectList() = binding.addMBList.adapter.let {
        if (it is AddMusicListAdapter) {
            it.selectList
        } else null
    }
}