package com.protone.seen

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.mediamodle.note.entity.RichNoteStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.SpanStates
import com.protone.seen.adapter.RichNoteAdapter
import com.protone.seen.databinding.NoteEditLayoutBinding

class NoteEditSeen(context: Context) : Seen<NoteEditSeen.NoteEditEvent>(context) {

    enum class NoteEditEvent {
        Confirm,
        Finish
    }

    private val binding = NoteEditLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    init {
        binding.self = this
        binding.noteEditRichView.apply {
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
                    ), RichPhotoStates(Uri.parse("asd"), null, "", null)
                )
            )
        }
    }

    override fun offer(event: NoteEditEvent) {
        viewEvent.offer(event)
    }
}