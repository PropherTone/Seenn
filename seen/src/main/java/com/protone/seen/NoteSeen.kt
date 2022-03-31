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

    init {
        binding.note.apply {
            layoutManager = LinearLayoutManager(context)
            val listOf = listOf(
                SpanStates(1, 3, SpanStates.Spans.ForegroundColorSpan, iColor = "#48a1ff"),
                SpanStates(1, 3, SpanStates.Spans.StrikeThroughSpan),
                SpanStates(4, 7, SpanStates.Spans.ForegroundColorSpan, iColor = Color.RED),
                SpanStates(8, 12, SpanStates.Spans.UnderlineSpan),
                SpanStates(13, 15, SpanStates.Spans.ForegroundColorSpan),
                SpanStates(16, 18, SpanStates.Spans.ForegroundColorSpan)
            )
            adapter = RichNoteAdapter(
                context, true, arrayListOf(
                    RichNoteStates(
                        context.getString(R.string.huge_text),
                        listOf
                    ), RichPhotoStates(Uri.parse("asd"),null,"",null)
                )
            )
        }
    }
}