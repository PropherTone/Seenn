package com.protone.seen

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
import com.protone.mediamodle.note.entity.RichNoteStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.SpanStates
import com.protone.seen.adapter.RichNoteAdapter
import com.protone.seen.databinding.NoteLayoutBinding

class NoteSeen(context: Context) : Seen<NoteSeen.NoteEvent>(context) {

    enum class NoteEvent {

    }

    val binding = NoteLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        initToolBar()

    }

    override fun offer(event: NoteEvent) {

    }
}