package com.protone.seenn

import android.net.Uri
import com.protone.api.context.intent
import com.protone.api.context.onBackground
import com.protone.api.json.toJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.mediamodle.Galley
import com.protone.seen.NoteViewSeen
import com.protone.seen.customView.richText.RichNoteView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.stream.Collectors

class NoteViewActivity : BaseActivity<NoteViewSeen>() {

    companion object {
        const val NOTE_NAME = "NOTE_NAME"
    }

    private val noteQueue = ArrayDeque<String>()

    override suspend fun main() {
        bindMusicService {}

        val noteViewSeen = intent.getStringExtra(NOTE_NAME)?.let {
            noteQueue.offer(it)
            initSeen(noteQueue.poll())
        } ?: NoteViewSeen(this)

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                noteViewSeen.viewEvent.onReceive {
                    when (it) {
                        NoteViewSeen.NoteViewEvent.Finish ->
                            if (noteQueue.isNotEmpty())
                                setContentSeen(initSeen(noteQueue.poll()))
                            else finish()
                        NoteViewSeen.NoteViewEvent.Next -> setContentSeen(initSeen(noteQueue.peek()))
                    }
                }
            }
        }
    }

    private suspend fun initSeen(noteName: String?) = NoteViewSeen(this@NoteViewActivity).apply {
        setContentSeen(this)
        if (noteName == null) return@apply
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<Note?> { co ->
                co.resumeWith(Result.success(DataBaseDAOHelper.getNoteByName(noteName)))
            }
        }.let { note ->
            if (note != null) {
                initNote(note, object : RichNoteView.IRichListener {
                    override fun play(uri: Uri, progress: Long) {
                        onBackground {
                            DataBaseDAOHelper.getMusicByUri(uri)
                                ?.let {
                                    setMusicDuration(it.duration)
                                    binder.play(it, progress)
                                }
                        }
                        binder.getPosition().observe(this@NoteViewActivity) {
                            setMusicProgress(it)
                        }
                    }

                    override fun pause() {
                        binder.pause()
                    }

                    override fun jumpTo(note: String) {
                        noteQueue.offer(note)
                        offer(NoteViewSeen.NoteViewEvent.Next)
                    }

                    override fun openImage(uri: Uri, name: String) {
                        val collect = Galley.allPhoto
                            ?.stream()
                            ?.filter { media -> media.uri == uri }
                            ?.collect(Collectors.toList())
                        if (collect != null && collect.size > 0) {
                            startActivity(GalleyViewActivity::class.intent.apply {
                                putExtra(GalleyViewActivity.MEDIA, collect[0].toJson())
                                putExtra(GalleyViewActivity.TYPE, false)
                            })
                        } else toast(getString(R.string.none))
                    }

                })
            } else toast(getString(R.string.come_up_unknown_error))
        }
    }
}