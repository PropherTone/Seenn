package com.protone.seen

import android.content.Context
import android.view.View
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.mediamodle.note.entity.*
import com.protone.mediamodle.note.spans.ISpanForUse
import com.protone.seen.customView.ColorPopWindow
import com.protone.seen.databinding.NoteEditLayoutBinding
import com.protone.mediamodle.note.entity.SpanStates

class NoteEditSeen(context: Context) : Seen<NoteEditSeen.NoteEditEvent>(context), ISpanForUse {

    enum class NoteEditEvent {
        Confirm,
        Finish,
        PickImage,
        PickVideo,
        PickMusic
    }

    private val binding = NoteEditLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.toolbar

    init {
        initToolBar()
        binding.self = this
        binding.noteEditRichNote.apply {
            isEditable = true
            val listOf = arrayListOf(
                SpanStates(1, 3, SpanStates.Spans.ForegroundColorSpan, iColor = "#48a1ff"),
                SpanStates(1, 3, SpanStates.Spans.StrikeThroughSpan),
                SpanStates(4, 7, SpanStates.Spans.ForegroundColorSpan, iColor = "#48a1ff"),
                SpanStates(8, 12, SpanStates.Spans.UnderlineSpan),
                SpanStates(13, 15, SpanStates.Spans.ForegroundColorSpan),
                SpanStates(16, 18, SpanStates.Spans.ForegroundColorSpan)
            )
            setRichList(listOf(RichNoteStates(context.getString(R.string.huge_text), listOf)))
        }
    }

    override fun offer(event: NoteEditEvent) {
        viewEvent.offer(event)
    }

    override fun setBold() = binding.noteEditRichNote.setBold()

    override fun setItalic() = binding.noteEditRichNote.setItalic()

    override fun setSize() = binding.noteEditRichNote.setSize(12)

    override fun setUnderlined() = binding.noteEditRichNote.setUnderlined()

    override fun setStrikethrough() = binding.noteEditRichNote.setStrikethrough()

    override fun insertImage() = offer(NoteEditEvent.PickImage)

    fun insertImage(photo: RichPhotoStates) = binding.noteEditRichNote.insertImage(photo)

    override fun insertVideo() = offer(NoteEditEvent.PickVideo)

    fun insertVideo(video: RichVideoStates) = binding.noteEditRichNote.insertVideo(video)

    override fun insertMusic() = offer(NoteEditEvent.PickMusic)

    fun insertMusic(music: RichMusicStates) = binding.noteEditRichNote.insertMusic(music)

    suspend fun indexRichNote(): Pair<Int, String> {
//        val indexRichNote = binding.noteEditRichNote.indexRichNote()
//        var first = indexRichNote.first
//        val second = indexRichNote.second
//        val statesStrings = second.jsonToList(String::class.java)
//        var listSize = statesStrings.size - 1
//        val richList = arrayListOf<RichStates>()
//        while (first > 0) {
//            richList.add(
//                when (first % 10) {
//                    RichNoteView.PHOTO -> {
//                        statesStrings[listSize--].toEntity(RichPhotoStates::class.java)
//                    }
//                    RichNoteView.MUSIC -> {
//                        statesStrings[listSize--].toEntity(RichMusicStates::class.java)
//                    }
//                    RichNoteView.VIDEO -> {
//                        statesStrings[listSize--].toEntity(RichVideoStates::class.java)
//                    }
//                    else -> {
//                        val toEntity = statesStrings[listSize--].toEntity(RichNoteSer::class.java)
//                        val toEntity1 = toEntity.spans.jsonToList(SpanStates::class.java)
//                        RichNoteStates(toEntity.text,toEntity1)
//                    }
//                }
//            )
//            first /= 10
//        }
        //TODO Only string color span can be saved
//        binding.noteEditRichNote.setRichList(richList)

        return binding.noteEditRichNote.indexRichNote()
    }

    override fun setColor() {
        ColorPopWindow(context).startPopWindow(binding.noteEditTool) {
            binding.noteEditRichNote.setColor(it)
        }
    }

}