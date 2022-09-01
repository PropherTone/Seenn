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
import com.protone.worker.database.MediaAction
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

        refreshList()

        collectNoteEvent {
            when (it) {
                is MediaAction.OnNoteDeleted -> {
                    deleteNoteCache(it.note)
                    getNoteListAdapter()?.deleteNote(it.note)
                }
                is MediaAction.OnNoteUpdated ->
                    getNote(it.note.title)?.let { note -> getNoteListAdapter()?.updateNote(note) }
                is MediaAction.OnNoteInserted ->
                    getNote(it.note.title)?.let { note -> getNoteListAdapter()?.insertNote(note) }
                is MediaAction.OnNoteDirInserted -> {}
                is MediaAction.OnNoteDirDeleted -> {}
                else -> Unit
            }
        }

        onViewEvent {
            when (it) {
                NoteViewModel.NoteViewEvent.RefreshList -> refreshList()
                NoteViewModel.NoteViewEvent.AddBucket -> addBucket()
                NoteViewModel.NoteViewEvent.Refresh -> refresh()
                NoteViewModel.NoteViewEvent.HandleBucketEvent -> handleBucketEvent()
            }
        }
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
        getNoteTypeAdapter()?.addNote = it
    }

    private fun onTypeSelected(it: ((String?) -> Unit)?) {
        getNoteTypeAdapter()?.onTypeSelected = it
    }

    private fun refreshNoteList(list: List<Note>) {
        getNoteListAdapter()?.setNoteList(list)
    }

    private fun refreshNoteType(list: List<NoteDir>) {
        getNoteTypeAdapter()?.setNoteTypeList(list)
    }

    private fun getNoteListAdapter(): NoteListAdapter? {
        return (binding.noteList.adapter as NoteListAdapter?)
    }

    private fun getNoteTypeAdapter(): NoteTypeListAdapter? {
        return (binding.noteBucketList.adapter as NoteTypeListAdapter?)
    }
}