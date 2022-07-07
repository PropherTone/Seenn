package com.protone.seenn.activity

import android.net.Uri
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.getString
import com.protone.api.json.toJson
import com.protone.api.toast
import com.protone.database.room.entity.Note
import com.protone.seen.customView.richText.RichNoteView
import com.protone.seenn.R
import com.protone.seenn.databinding.NoteViewActivityBinding
import com.protone.seenn.viewModel.GalleyViewViewModel
import com.protone.seenn.viewModel.NoteEditViewModel
import com.protone.seenn.viewModel.NoteViewViewModel
import kotlinx.coroutines.launch

class NoteViewActivity : BaseActivity<NoteViewActivityBinding, NoteViewViewModel>(true) {
    override val viewModel: NoteViewViewModel by viewModels()

    override fun initView() {
        binding = NoteViewActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitNavigationBarUsePadding(binding.root)
    }

    override suspend fun NoteViewViewModel.init() {
        bindMusicService {}

        intent.getStringExtra(NoteViewViewModel.NOTE_NAME)?.let {
            noteQueue.offer(it)
            initSeen(noteQueue.poll())
        }
    }

    override suspend fun onViewEvent(event: String) {
        when (event) {
            NoteViewViewModel.ViewEvent.Next.name -> viewModel.initSeen(viewModel.noteQueue.poll())
            NoteViewViewModel.ViewEvent.Edit.name -> edit()
        }
    }

    fun sendNext() {
        sendViewEvent(NoteViewViewModel.ViewEvent.Next.name)
    }

    fun sendEdit() {
        sendViewEvent(NoteViewViewModel.ViewEvent.Edit.name)
    }

    override fun finish() {
        if (viewModel.noteQueue.isNotEmpty()) {
            sendNext()
        } else super.finish()
    }

    private suspend fun NoteViewViewModel.initSeen(noteName: String?) {
        if (noteName == null) return
        val note = getNoteByName(noteName)
        if (note != null) {
            initNote(note, object : RichNoteView.IRichListener {
                override fun play(uri: Uri, progress: Long) {
                    launch {
                        getMusicByUri(uri)?.let {
                            setMusicDuration(it.duration)
                            binder.play(it)
                            binder.setProgress(progress)
                        }
                    }
                    binder.onProgress().observe(this@NoteViewActivity) {
                        setMusicProgress(it)
                    }
                }

                override fun pause() {
                    binder.pause()
                }

                override fun jumpTo(note: String) {
                    noteQueue.offer(note)
                    sendNext()
                }

                override fun open(uri: Uri, name: String, isVideo: Boolean) {
                    launch {
                        val collect = filterMedia(uri, isVideo)
                        if (collect != null && collect.size > 0) {
                            startActivity(GalleyViewActivity::class.intent.apply {
                                putExtra(GalleyViewViewModel.MEDIA, collect[0].toJson())
                                putExtra(GalleyViewViewModel.TYPE, isVideo)
                            })
                        } else R.string.none.getString().toast()
                    }
                }

            })
        } else toast(getString(R.string.come_up_unknown_error))
    }

    private suspend fun edit() = viewModel.apply {
        val re = startActivityForResult(
            NoteEditActivity::class.intent.also { intent ->
                intent.putExtra(
                    NoteEditViewModel.NOTE,
                    this@NoteViewActivity.intent.getStringExtra(NoteViewViewModel.NOTE_NAME)
                )
            }
        )
        if (re == null) {
            R.string.none.getString().toast()
            return@apply
        }
        if (re.resultCode != RESULT_OK) return@apply
        intent.getStringExtra(NoteViewViewModel.NOTE_NAME)?.let { name ->
            noteQueue.remove(name)
            noteQueue.offer(name)
            initSeen(noteQueue.poll())
        }
    }

    private fun initNote(note: Note, listener: RichNoteView.IRichListener) {
        binding.apply {
            noteEditTitle.text = note.title
            Glide.with(this@NoteViewActivity)
                .asDrawable()
                .load(note.imagePath)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(noteEditIcon)
            noteEditRichNote.isEditable = false
            noteEditRichNote.setRichList(note.richCode, note.text)
            noteEditRichNote.iRichListener = listener
        }
    }

    fun setMusicProgress(progress: Long) {
        binding.noteEditRichNote.setMusicProgress(progress)
    }

    fun setMusicDuration(duration: Long) {
        binding.noteEditRichNote.setMusicDuration(duration)
    }
}