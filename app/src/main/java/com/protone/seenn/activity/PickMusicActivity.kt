package com.protone.seenn.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.SearchModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.MUSIC_PLAY
import com.protone.api.context.linkInput
import com.protone.api.context.root
import com.protone.api.entity.Music
import com.protone.seenn.R
import com.protone.seenn.broadcast.musicBroadCastManager
import com.protone.seenn.databinding.PickMusicActivityBinding
import com.protone.ui.adapter.AddMusicListAdapter
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.PickMusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickMusicActivity :
    BaseActivity<PickMusicActivityBinding, PickMusicViewModel, BaseViewModel.ViewEvent>(true) {
    override val viewModel: PickMusicViewModel by viewModels()

    override fun createView(): PickMusicActivityBinding {
        return PickMusicActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@PickMusicActivity
            fitStatuesBar(root)
            linkInput(addMBList, addMBSearch)
        }
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
        val searchModel = SearchModel(binding.addMBSearch) {
            viewModel.query(getInput())
        }
        onFinish = {
            searchModel.destroy()
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
        launch(Dispatchers.Default) {
            refreshList(filterData(input))
        }
    }

    private suspend fun refreshList(list: MutableList<Music>) = withContext(Dispatchers.Main) {
        binding.addMBList.adapter.let {
            if (it is AddMusicListAdapter) {
                binding.addMBList.swapAdapter(
                    newAdapter(
                        it.bucket,
                        it.mode,
                        it.adapterDataBaseProxy,
                        list
                    ), true
                )
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
            adapter = newAdapter(
                bucket,
                mode,
                object : AddMusicListAdapter.AddMusicListAdapterDataProxy {
                    override suspend fun getMusicWithMusicBucket(bucket: String): Collection<Music> =
                        viewModel.getMusicWithMusicBucket(bucket)

                    override fun deleteMusicWithMusicBucket(
                        musicBaseId: Long,
                        musicBucket: String
                    ) = viewModel.deleteMusicWithMusicBucket(musicBaseId, musicBucket)

                    override fun play(music: Music) {
                        musicBroadCastManager.sendBroadcast(Intent(MUSIC_PLAY))
                    }

                    override suspend fun insertMusicWithMusicBucket(
                        musicBaseId: Long,
                        bucket: String
                    ): Long = viewModel.insertMusicWithMusicBucket(musicBaseId, bucket)
                },
                viewModel.getMusics()
            )
        }
    }

    private fun newAdapter(
        bucket: String,
        mode: String,
        proxy: AddMusicListAdapter.AddMusicListAdapterDataProxy,
        musics: MutableList<Music>
    ) = AddMusicListAdapter(
        this,
        bucket,
        mode,
        proxy
    ).apply { musicList = musics }

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