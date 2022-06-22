package com.protone.seenn

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.json.toUriJson
import com.protone.api.toDateString
import com.protone.api.toDrawable
import com.protone.api.toMediaBitmapByteArray
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Note
import com.protone.mediamodle.GalleyHelper
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.seen.GalleySeen
import com.protone.seen.NoteEditSeen
import com.protone.seen.PickMusicSeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class NoteEditActivity : BaseActivity<NoteEditSeen>() {

    private var savedIconPath: String = ""
    private var iconUri: Uri? = null

    private var allNote: MutableList<String>? = null

    private suspend fun getAllNote() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<MutableList<String>> {
            val notes = DataBaseDAOHelper.getAllNote()
            val list = mutableListOf<String>()
            notes?.forEach { note ->
                list.add(note.title)
            }
            it.resumeWith(Result.success(list))
        }
    }

    companion object {
        const val NOTE_TYPE = "NoteType"
        const val NOTE = "Note"
    }

    override suspend fun main() {
        val noteEditSeen = NoteEditSeen(this)
        val noteName = intent.getStringExtra(NOTE)

        var onEdit = false
        val noteByName = noteName?.let {
            withContext(Dispatchers.IO) {
                DataBaseDAOHelper.getNoteByName(it)
            }?.let { n ->
                noteEditSeen.title = n.title
                noteEditSeen.initEditor(n.getRichCode(), n.getText())
                onEdit = true
                n
            }
        }
        setContentSeen(noteEditSeen)
        while (isActive) {
            select<Unit> {
                event.onReceive {}
                noteEditSeen.viewEvent.onReceive {
                    when (it) {
                        NoteEditSeen.NoteEditEvent.Confirm -> {
                            val indexedRichNote = noteEditSeen.indexRichNote()
                            if (noteEditSeen.title.isEmpty()) {
                                toast(getString(R.string.enter_title))
                                return@onReceive
                            }
                            noteEditSeen.showProgress(true)
                            val note = withContext(Dispatchers.IO) {
                                Note(
                                    noteEditSeen.title,
                                    indexedRichNote.second,
                                    if (iconUri != null) suspendCancellableCoroutine { co ->
                                        GalleyHelper.saveIconToLocal(
                                            noteEditSeen.title,
                                            iconUri?.toMediaBitmapByteArray()
                                        ) { s ->
                                            if (!s.isNullOrEmpty()) {
                                                savedIconPath = s
                                                savedIconPath.toDrawable(this@NoteEditActivity) { dra ->
                                                    noteEditSeen.setNoteIcon(dra)
                                                    co.resumeWith(Result.success(savedIconPath))
                                                }
                                            } else {
                                                toast(getString(R.string.failed_upload_image))
                                                co.resumeWith(Result.success(iconUri!!.toUriJson()))
                                            }
                                            noteEditSeen.showProgress(false)
                                        }
                                    } else "",
                                    System.currentTimeMillis(),
                                    mutableListOf(intent.getStringExtra(NOTE_TYPE)),
                                    indexedRichNote.first
                                )
                            }
                            if (onEdit) withContext(Dispatchers.IO) {
                                if (noteName == null) {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                    return@withContext
                                }
                                if (noteByName == null) {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                    return@withContext
                                }
                                noteByName.title = note.title
                                noteByName.type = note.type
                                noteByName.text = note.text
                                noteByName.imagePath = note.imagePath
                                noteByName.richCode = note.richCode
                                noteByName.time = note.time
                                DataBaseDAOHelper.updateNote(noteByName).let { re ->
                                    if (re == null && re == -1) {
                                        DataBaseDAOHelper.insertNoteCB(noteByName) { result, _ ->
                                            if (result) {
                                                DataBaseDAOHelper.deleteNote(noteByName)
                                                setResult(RESULT_OK, Intent().also { intent ->
                                                    intent.putExtra(
                                                        NoteViewActivity.NOTE_NAME,
                                                        noteByName.title
                                                    )
                                                })
                                                finish()
                                            } else toast(getString(R.string.failed_msg))
                                        }
                                    } else {
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                }
                            } else DataBaseDAOHelper.insertNoteCB(note) { re, _ ->
                                if (re) finish() else toast(getString(R.string.failed_msg))
                            }
                        }
                        NoteEditSeen.NoteEditEvent.Finish -> finish()
                        NoteEditSeen.NoteEditEvent.PickImage -> startGalleyPick(true)?.let { re ->
                            noteEditSeen.insertImage(
                                RichPhotoStates(
                                    re.uri, re.name, null,
                                    re.date.toDateString().toString()
                                )
                            )
                            re.apply {
                                if (notes == null) notes = mutableListOf()
                                (notes as MutableList<String>).add(noteEditSeen.title)
                            }
                            withContext(Dispatchers.IO) {
                                DataBaseDAOHelper.updateSignedMedia(re)
                            }
                        }
                        NoteEditSeen.NoteEditEvent.PickVideo -> startGalleyPick(false)?.let { re ->
                            if (allNote == null) allNote = getAllNote()
                            noteEditSeen.insertVideo(re.uri, allNote!!)
                        }
                        NoteEditSeen.NoteEditEvent.PickMusic -> startActivityForResult(
                            ActivityResultContracts.StartActivityForResult(),
                            PickMusicActivity::class.intent.apply {
                                putExtra(PickMusicSeen.MODE, PickMusicSeen.PICK_MUSIC)
                            }
                        )?.let { re ->
                            re.data?.data?.let { uri ->
                                if (allNote == null) allNote = getAllNote()
                                val title = withContext(Dispatchers.IO) {
                                    suspendCancellableCoroutine<String> { co ->
                                        val musicByUri = DataBaseDAOHelper.getMusicByUri(uri)
                                        co.resumeWith(
                                            Result.success(musicByUri?.title ?: "^ ^")
                                        )
                                    }
                                }
                                noteEditSeen.insertMusic(uri, allNote!!, title)
                            }
                        }
                        NoteEditSeen.NoteEditEvent.PickIcon -> startGalleyPick(true)?.let { re ->
                            noteEditSeen.setNoteIconCache(re.uri)
                            iconUri = re.uri
                        }

                    }

                }
            }

        }

    }

    private suspend fun startGalleyPick(isPhoto: Boolean) = startActivityForResult(
        ActivityResultContracts.StartActivityForResult(),
        GalleyActivity::class.intent.apply {
            putExtra(
                GalleyActivity.CHOOSE_MODE,
                if (isPhoto) GalleySeen.CHOOSE_PHOTO else GalleySeen.CHOOSE_VIDEO
            )
        })?.let { re ->
        re.data?.getStringExtra(GalleyActivity.GALLEY_DATA)?.toEntity(GalleyMedia::class.java)
    }

}