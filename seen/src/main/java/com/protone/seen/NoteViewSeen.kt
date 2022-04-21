package com.protone.seen

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.json.jsonToList
import com.protone.api.json.toEntity
import com.protone.api.toDrawable
import com.protone.database.room.entity.Note
import com.protone.mediamodle.note.entity.*
import com.protone.seen.customView.richText.RichNoteView
import com.protone.seen.databinding.NoteViewerLayoutBinding

class NoteViewSeen(context: Context) : Seen<NoteViewSeen.NoteViewEvent>(context) {

    enum class NoteViewEvent {
        Finish
    }

    private val binding =
        NoteViewerLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    override fun offer(event: NoteViewEvent) {

    }

    override fun getToolBar(): View = binding.root

    init {
        setNavigation()
        binding.apply {
            self = this@NoteViewSeen
        }
    }

    fun initNote(note: Note) {
        binding.apply {
            noteEditTitle.text = note.title
            note.imagePath.toDrawable(context) {
                Glide.with(context)
                    .asDrawable()
                    .load(it).skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(noteEditIcon)
            }
            noteEditRichNote.isEditable = false
            noteEditRichNote.setRichList(note.richCode, note.text)
        }
    }
}