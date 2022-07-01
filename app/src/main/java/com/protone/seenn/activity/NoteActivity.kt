package com.protone.seenn.activity

import android.animation.ValueAnimator
import android.transition.TransitionManager
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteType
import com.protone.seen.R
import com.protone.seen.adapter.NoteListAdapter
import com.protone.seen.adapter.NoteTypeListAdapter
import com.protone.seen.dialog.TitleDialog
import com.protone.seenn.NoteEditActivity
import com.protone.seenn.NoteViewActivity
import com.protone.seenn.databinding.NoteActivityBinding
import com.protone.seenn.viewModel.NoteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class NoteActivity : BaseActivity<NoteActivityBinding, NoteViewModel>(true) {
    override val viewModel: NoteViewModel by viewModels()

    override suspend fun initView() {
        binding = NoteActivityBinding.inflate(layoutInflater, root, false)
        fitStatuesBar(binding.root)
        fitNavigationBar(binding.root)
        binding.activity = this
    }

    override suspend fun onViewEvent(event: String) {
        when (event) {
            NoteViewModel.ViewEvent.Init.name -> init()
            NoteViewModel.ViewEvent.RefreshList.name -> refreshList()
        }
    }

    override suspend fun init() {
        initList()
        binding.noteList.adapter.apply {
            this as NoteListAdapter
            this.noteListEventListener = object : NoteListAdapter.NoteListEvent {
                override fun onNote(title: String) {
                    startActivity(NoteViewActivity::class.intent.also {
                        it.putExtra(NoteViewActivity.NOTE_NAME, title)
                    })
                }

                override fun onDelete(note: Note) {
                    //TODO Delete note not IMP yet
                }
            }
        }
        addNoteType {
            startActivity(NoteEditActivity::class.intent.also { intent ->
                intent.putExtra(NoteEditActivity.NOTE_TYPE, it)
            })
        }
        onTypeSelected { type ->
            refreshNoteList(viewModel.getNoteList(type))
        }
    }

    override fun onResume() {
        super.onResume()
        sendViewEvent(NoteViewModel.ViewEvent.RefreshList.name)
    }

    fun addBucket() {
        TitleDialog(this@NoteActivity, getString(R.string.add_dir), "") { re ->
            if (re.isNotEmpty()) {
                launch(Dispatchers.IO) {
                    viewModel.insertNoteType(re, "").let { pair ->
                        if (pair.first) {
                            if (binding.noteBucketList.adapter is NoteTypeListAdapter)
                                (binding.noteBucketList.adapter as NoteTypeListAdapter)
                                    .insertNoteType(NoteType(pair.second, ""))
                        } else {
                            withContext(Dispatchers.Main) {
                                toast(getString(R.string.failed_msg))
                            }
                        }
                    }
                }
            } else {
                toast(getString(R.string.enter))
            }
        }
    }

    fun refresh() {
        handleBucketEvent()
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
        sendViewEvent(NoteViewModel.ViewEvent.Init.name)
        sendViewEvent(NoteViewModel.ViewEvent.RefreshList.name)
    }

    private suspend fun refreshList() {
        refreshNoteList(viewModel.queryAllNote())
        refreshNoteType(viewModel.queryAllNoteType())
    }

    private fun initList() {
        binding.apply {
            noteList.also {
                it.layoutManager = LinearLayoutManager(this@NoteActivity)
                it.adapter = NoteListAdapter(this@NoteActivity)
            }
            noteBucketList.also {
                it.layoutManager = LinearLayoutManager(this@NoteActivity)
                it.adapter = NoteTypeListAdapter(this@NoteActivity)
            }
        }
    }

    private fun addNoteType(it: ((String?) -> Unit)?) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).addNote = it
    }

    private fun onTypeSelected(it: ((String?) -> Unit)?) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).onTypeSelected = it
    }

    private fun refreshNoteList(list: List<Note>) {
        (binding.noteList.adapter as NoteListAdapter?)?.setNoteList(list)
    }

    private fun refreshNoteType(list: List<NoteType>) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).setNoteTypeList(list)
    }

    fun handleBucketEvent() {
        val progress = binding.noteContainer.progress
        ValueAnimator.ofFloat(progress, abs(progress - 1f)).apply {
            addUpdateListener {
                binding.noteContainer.progress = (it.animatedValue as Float)
                binding.noteBucket.progress = binding.noteContainer.progress
            }
        }.start()
    }
}