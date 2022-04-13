package com.protone.seen

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.mediamodle.note.entity.RichNoteStates
import com.protone.mediamodle.note.spans.ISpan
import com.protone.seen.adapter.RichNoteAdapter
import com.protone.seen.customView.ColorPopWindow
import com.protone.seen.databinding.NoteEditLayoutBinding

class NoteEditSeen(context: Context) : Seen<NoteEditSeen.NoteEditEvent>(context), ISpan {

    enum class NoteEditEvent {
        Confirm,
        Finish,
        PickImage
    }

    private val binding = NoteEditLayoutBinding.inflate(context.layoutInflater, context.root, true)

    private var richNoteAdapter: RichNoteAdapter? = null

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.toolbar

    init {
        initToolBar()
        binding.self = this
        binding.noteEditRichView.apply {
            layoutManager = LinearLayoutManager(context)
//            val listOf = listOf(
//                SpanStates(1, 3, SpanStates.Spans.ForegroundColorSpan, iColor = "#48a1ff"),
//                SpanStates(1, 3, SpanStates.Spans.StrikeThroughSpan),
//                SpanStates(4, 7, SpanStates.Spans.ForegroundColorSpan, iColor = Color.RED),
//                SpanStates(8, 12, SpanStates.Spans.UnderlineSpan),
//                SpanStates(13, 15, SpanStates.Spans.ForegroundColorSpan),
//                SpanStates(16, 18, SpanStates.Spans.ForegroundColorSpan)
//            )
            richNoteAdapter = RichNoteAdapter(
                context,
                true,
                arrayListOf(RichNoteStates("", arrayListOf()))
            )
            adapter = richNoteAdapter
        }
    }

    override fun offer(event: NoteEditEvent) {
        viewEvent.offer(event)
    }

    override fun setBold() {
        richNoteAdapter?.setBold()
    }

    override fun setItalic() {
        richNoteAdapter?.setItalic()
    }

    override fun setSize() {
        richNoteAdapter?.setSize(12)
    }

    override fun setUnderlined() {
        richNoteAdapter?.setUnderlined()
    }

    override fun setStrikethrough() {
        richNoteAdapter?.setStrikethrough()
    }

    override fun setColor() {
        ColorPopWindow(context).startPopWindow(binding.noteEditTool) {
            richNoteAdapter?.setColor(it)
        }
    }

    override fun setImage() {
        offer(NoteEditEvent.PickImage)
    }

    override fun setImage(uri: Uri, link: String?, name: String, date: String?) {
        richNoteAdapter?.setImage(uri, link, name, date)
    }

    suspend fun indexRichNote(){
        richNoteAdapter?.indexRichNote()
    }
}