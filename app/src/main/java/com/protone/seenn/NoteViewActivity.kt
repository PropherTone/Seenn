package com.protone.seenn

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.context.onBackground
import com.protone.api.json.toJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Note
import com.protone.seen.NoteViewSeen
import com.protone.seen.customView.richText.RichNoteView
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.stream.Collectors

class NoteViewActivity : BaseActivity<NoteViewSeen>() {

    companion object {
        const val NOTE_NAME = "NOTE_NAME"
    }

    private lateinit var noteViewSeen: NoteViewSeen
    private val noteQueue = ArrayDeque<String>()

    override suspend fun main() {
        bindMusicService {}

        noteViewSeen = intent.getStringExtra(NOTE_NAME)?.let {
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
                                initSeen(noteQueue.poll())
                            else finish()
                        NoteViewSeen.NoteViewEvent.Edit -> startActivityForResult(
                            ActivityResultContracts.StartActivityForResult(),
                            NoteEditActivity::class.intent.also { intent ->
                                intent.putExtra(
                                    NoteEditActivity.NOTE,
                                    this@NoteViewActivity.intent.getStringExtra(NOTE_NAME)
                                )
                            }
                        ).let { re ->
                            if (re == null) {
                                toast(getString(R.string.none))
                                return@let
                            }
                            if (re.resultCode == RESULT_OK)
                                intent.getStringExtra(NOTE_NAME)?.let { name ->
                                    noteQueue.remove(name)
                                    noteQueue.offer(name)
                                    initSeen(noteQueue.poll())
                                }
                        }
                        NoteViewSeen.NoteViewEvent.Next -> initSeen(noteQueue.peek())
                    }
                }
            }
        }
    }

    private suspend fun initSeen(noteName: String?) = NoteViewSeen(this@NoteViewActivity).apply {
        noteViewSeen = this
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
                        offer(NoteViewSeen.NoteViewEvent.Next)
                    }

                    override fun open(uri: Uri, name: String, isVideo: Boolean) {
                        launch {
                            val collect = withContext(Dispatchers.IO) {
                                DataBaseDAOHelper.getAllMediaByType(isVideo)
                            }?.stream()
                                ?.filter { media -> media.uri == uri }
                                ?.collect(Collectors.toList())
                            if (collect != null && collect.size > 0) {
                                startActivity(GalleyViewActivity::class.intent.apply {
                                    putExtra(GalleyViewActivity.MEDIA, collect[0].toJson())
                                    putExtra(GalleyViewActivity.TYPE, isVideo)
                                })
                            } else toast(getString(R.string.none))
                        }
                    }

                })
            } else toast(getString(R.string.come_up_unknown_error))
        }
    }
}