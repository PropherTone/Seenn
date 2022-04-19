package com.protone.seen

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.Note
import com.protone.mediamodle.note.entity.RichNoteStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.SpanStates
import com.protone.seen.adapter.NoteListAdapter
import com.protone.seen.adapter.NoteTypeListAdapter
import com.protone.seen.adapter.RichNoteAdapter
import com.protone.seen.databinding.NoteLayoutBinding
import kotlin.math.abs

class NoteSeen(context: Context) : Seen<NoteSeen.NoteEvent>(context), View.OnClickListener {

    enum class NoteEvent {

    }

    val binding = NoteLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        setNavigation()
        binding.apply {
            noteAction.setOnClickListener(this@NoteSeen)
        }
    }

    override fun offer(event: NoteEvent) {
        offer(event)
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

    fun addNoteType(it: ((String?) -> Unit)?) {
        (binding.noteBucketList.adapter as NoteTypeListAdapter?)?.addNoteType = it
    }

    fun onTypeSelected(it: ((String?) -> Unit)?) {
        (binding.noteBucketList.adapter as NoteTypeListAdapter?)?.onTypeSelected = it
    }

    fun refreshNoteList(list: List<Note>) {
        (binding.noteList.adapter as NoteListAdapter?)?.setNoteList(list)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.noteAction -> {
                handleBucketEvent()
            }
        }
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
}