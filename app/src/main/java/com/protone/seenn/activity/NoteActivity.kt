package com.protone.seenn.activity

import android.animation.ValueAnimator
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.seenn.databinding.NoteActivityBinding
import com.protone.ui.R
import com.protone.ui.adapter.NoteListAdapter
import com.protone.ui.adapter.NoteTypeListAdapter
import com.protone.ui.dialog.titleDialog
import com.protone.worker.viewModel.NoteEditViewModel
import com.protone.worker.viewModel.NoteViewModel
import com.protone.worker.viewModel.NoteViewViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class NoteActivity : BaseActivity<NoteActivityBinding, NoteViewModel>(true) {
    override val viewModel: NoteViewModel by viewModels()

    override fun createView(): View {
        binding = NoteActivityBinding.inflate(layoutInflater, root, false)
        fitStatuesBar(binding.root)
        binding.activity = this
        return binding.root
    }

    override suspend fun NoteViewModel.init() {
        initList()
        binding.noteList.adapter.apply {
            this as NoteListAdapter
            this.noteListEventListener = object : NoteListAdapter.NoteListEvent {
                override fun onNote(title: String) {
                    startActivity(NoteViewActivity::class.intent.also {
                        it.putExtra(NoteViewViewModel.NOTE_NAME, title)
                    })
                }

                override fun onDelete(note: Note) {
                    titleDialog(R.string.delete.getString(), R.string.delete.getString()) {
                        this@init.deleteNote(note)
                        deleteNote(note)
                    }
                }
            }
        }
        addNoteType {
            startActivity(NoteEditActivity::class.intent.putExtra(NoteEditViewModel.NOTE_DIR, it))
        }
        onTypeSelected { type ->
            refreshNoteList(viewModel.getNoteList(type))
        }

        onViewEvent {
            when (it) {
                NoteViewModel.NoteViewEvent.Init -> init()
                NoteViewModel.NoteViewEvent.RefreshList -> refreshList()
                NoteViewModel.NoteViewEvent.AddBucket -> addBucket()
                NoteViewModel.NoteViewEvent.Refresh -> refresh()
                NoteViewModel.NoteViewEvent.HandleBucketEvent-> handleBucketEvent()
            }
        }
    }

    override suspend fun doResume() {
        sendViewEvent(NoteViewModel.NoteViewEvent.RefreshList)
    }

    private fun addBucket() {
        titleDialog(getString(R.string.add_dir), "") { re ->
            if (re.isNotEmpty()) {
                launch {
                    viewModel.insertNoteDir(re, "").let { pair ->
                        if (pair.first) {
                            (binding.noteBucketList.adapter as NoteTypeListAdapter)
                                .insertNoteDir(NoteDir(pair.second, ""))
                        } else {
                            R.string.failed_msg.getString().toast()
                        }
                    }
                }
            } else {
                R.string.enter.getString().toast()
            }
        }
    }

    private fun refresh() {
        handleBucketEvent()
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
        sendViewEvent(NoteViewModel.NoteViewEvent.Init)
        sendViewEvent(NoteViewModel.NoteViewEvent.RefreshList)
    }

    private fun handleBucketEvent() {
        val progress = binding.noteContainer.progress
        ValueAnimator.ofFloat(progress, abs(progress - 1f)).apply {
            addUpdateListener {
                binding.noteContainer.progress = (it.animatedValue as Float)
                binding.noteBucket.progress = binding.noteContainer.progress
            }
        }.start()
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

    private fun refreshNoteType(list: List<NoteDir>) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).setNoteTypeList(list)
    }
}