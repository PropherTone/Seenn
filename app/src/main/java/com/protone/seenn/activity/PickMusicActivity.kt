package com.protone.seenn.activity

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.SearchModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.linkInput
import com.protone.api.context.root
import com.protone.api.entity.Music
import com.protone.seenn.R
import com.protone.seenn.databinding.PickMusicActivityBinding
import com.protone.ui.adapter.AddMusicListAdapter
import com.protone.worker.viewModel.PickMusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickMusicActivity : BaseActivity<PickMusicActivityBinding, PickMusicViewModel>(true) {
    override val viewModel: PickMusicViewModel by viewModels()

    override fun createView(): View {
        binding = PickMusicActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBar(binding.root)
        linkInput(binding.addMBList, binding.addMBSearch)
        return binding.root
    }

    override suspend fun PickMusicViewModel.init() {
        val mode = intent.getStringExtra(PickMusicViewModel.MODE)

        val bucket = when (mode) {
            PickMusicViewModel.PICK_MUSIC -> R.string.all_music.getString()
            else -> intent.getStringExtra(PickMusicViewModel.BUCKET_NAME)
        }

        if (bucket != null) {
            initSeen(bucket, mode ?: PickMusicViewModel.ADD_BUCKET)
        } else {
            R.string.no_bucket.getString().toast()
            finish()
        }

        getList()?.let { data.addAll(it) }
        SearchModel(binding.addMBSearch) {
            viewModel.query(getInput())
        }
    }

    fun confirm() {
        val selectList = getSelectList()
        if (selectList != null && selectList.size > 0) {
            setResult(RESULT_OK, Intent().apply {
                data = selectList[0].uri
            })
        } else R.string.cancel.getString().toast()
        finish()
    }

    private fun PickMusicViewModel.query(input: String) {
        if (input.isEmpty()) return
        launch(Dispatchers.Default) {
            refreshList(filterData(input))
        }
    }

    private suspend fun refreshList(list: MutableList<Music>) = withContext(Dispatchers.Main) {
        binding.addMBList.adapter.let {
            if (it is AddMusicListAdapter) {
                it.noticeDataUpdate(list)
            }
        }
    }

    private suspend fun initSeen(bucket: String, mode: String) {
        binding.addMBConfirm.also {
            it.isGone = mode == PickMusicViewModel.ADD_BUCKET
            binding.addMBLeave.isGone = !it.isGone
        }
        binding.addMBList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter =
                AddMusicListAdapter(context, bucket, mode != PickMusicViewModel.PICK_MUSIC).apply {
                    musicList = viewModel.getMusics()
                }
        }
    }

    private fun getList(): MutableList<Music>? = binding.addMBList.adapter.let {
        if (it is AddMusicListAdapter) {
            return it.musicList
        } else null
    }

    private fun getSelectList() = binding.addMBList.adapter.let {
        if (it is AddMusicListAdapter) {
            it.selectList
        } else null
    }
}