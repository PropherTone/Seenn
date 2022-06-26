package com.protone.seen

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.TAG
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteType
import com.protone.seen.adapter.NoteListAdapter
import com.protone.seen.adapter.NoteTypeListAdapter
import com.protone.seen.databinding.NoteLayoutBinding
import kotlin.math.abs

class NoteSeen(context: Context) : Seen<NoteSeen.NoteEvent>(context) {

    enum class NoteEvent {
        Finish,
        AddBucket,
        Refresh
    }

    val binding = NoteLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        setNavigation()
        binding.self = this
    }

    override fun offer(event: NoteEvent) {
        viewEvent.trySend(event)
    }

    fun initList() {
        binding.apply {
            noteList.also {
                it.layoutManager = LinearLayoutManager(context)
                it.adapter = NoteListAdapter(context)
            }
            noteBucketList.also {
                it.layoutManager = LinearLayoutManager(context)
                it.adapter = NoteTypeListAdapter(context)
            }
        }
    }

    fun setNoteClk(it: ((String) -> Unit)?) {
        (binding.noteList.adapter as NoteListAdapter?)?.noteClk = it
    }

    fun addNoteType(it: ((String?) -> Unit)?) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).addNote = it
    }

    fun onTypeSelected(it: ((String?) -> Unit)?) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).onTypeSelected = it
    }

    fun refreshNoteList(list: List<Note>) {
        (binding.noteList.adapter as NoteListAdapter?)?.setNoteList(list)
    }

    fun refreshNoteType(list: List<NoteType>) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).setNoteTypeList(list)
    }

    fun insertNoteType(noteType: NoteType) {
        if (binding.noteBucketList.adapter is NoteTypeListAdapter)
            (binding.noteBucketList.adapter as NoteTypeListAdapter).insertNoteType(noteType)
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