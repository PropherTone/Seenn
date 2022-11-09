package com.protone.seenn.activity

import android.animation.ValueAnimator
import android.transition.TransitionManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.TAG
import com.protone.api.baseType.getString
import com.protone.api.baseType.launchDefault
import com.protone.api.baseType.launchIO
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.seenn.databinding.NoteActivityBinding
import com.protone.ui.R
import com.protone.ui.adapter.NoteListAdapter
import com.protone.ui.adapter.NoteListAdapterTemp
import com.protone.ui.adapter.NoteTypeListAdapter
import com.protone.ui.dialog.titleDialog
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.MediaAction
import com.protone.worker.viewModel.NoteEditViewModel
import com.protone.worker.viewModel.NoteViewModel
import com.protone.worker.viewModel.NoteViewViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs

class NoteActivity :
    BaseActivity<NoteActivityBinding, NoteViewModel, NoteViewModel.NoteViewEvent>(true) {
    override val viewModel: NoteViewModel by viewModels()

    override fun createView(): NoteActivityBinding {
        return NoteActivityBinding.inflate(layoutInflater, root, false).apply {
            fitStatuesBar(root)
            activity = this@NoteActivity
        }
    }

    override suspend fun NoteViewModel.init() {
        initList()
        binding.noteList.adapter.apply {
            this as NoteListAdapterTemp
            this.noteListEventListener = object : NoteListAdapterTemp.NoteListEvent {
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
            launch {
                refreshNoteList(viewModel.getNoteList(type))
            }
        }

        refreshList()

        collectNoteEvent {
            when (it) {
                is MediaAction.OnNoteDeleted -> deleteNoteCache(it.note)
                else -> Unit
            }
        }

        watchNotes {
            getNoteListAdapter()?.submitList(it)
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
                            getNoteTypeAdapter()?.insertNoteDir(pair.second)
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
                it.adapter = NoteListAdapterTemp(this@NoteActivity)
            }
            noteBucketList.also {
                it.layoutManager = LinearLayoutManager(this@NoteActivity)
                it.adapter = NoteTypeListAdapter(this@NoteActivity,
                    object : NoteTypeListAdapter.NoteTypeListAdapterDataProxy {
                        override fun deleteNoteDir(noteType: NoteDir) {
                            viewModel.deleteNoteDir(noteType)
                        }
                    })
            }
        }
    }

    private fun addNoteType(it: ((String?) -> Unit)?) {
        getNoteTypeAdapter()?.addNote = it
    }

    private fun onTypeSelected(it: ((NoteDir) -> Unit)?) {
        getNoteTypeAdapter()?.onTypeSelected = it
    }

    private fun refreshNoteList(list: List<Note>) {
        getNoteListAdapter()?.submitList(list)
    }

    private fun refreshNoteType(list: List<NoteDir>) {
        getNoteTypeAdapter()?.setNoteTypeList(list)
    }

    private fun getNoteListAdapter(): NoteListAdapterTemp? {
        return (binding.noteList.adapter as NoteListAdapterTemp?)
    }

    private fun getNoteTypeAdapter(): NoteTypeListAdapter? {
        return (binding.noteBucketList.adapter as NoteTypeListAdapter?)
    }
}