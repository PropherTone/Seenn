package com.protone.seenn

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.json.toUriJson
import com.protone.api.toDate
import com.protone.api.toDrawable
import com.protone.api.toMediaBitmapByteArray
import com.protone.api.todayTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Note
import com.protone.mediamodle.GalleyHelper
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.RichVideoStates
import com.protone.seen.AddMusic2BucketSeen
import com.protone.seen.GalleySeen
import com.protone.seen.NoteEditSeen
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
            notes?.forEach { note->
                list.add(note.title)
            }
            it.resumeWith(Result.success(list))
        }
    }

    companion object {
        const val NOTE_TYPE = "NoteType"
    }

    override suspend fun main() {
        val noteEditSeen = NoteEditSeen(this)
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
                            DataBaseDAOHelper.insertNoteCB(
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
                                    todayTime,
                                    intent.getStringExtra(NOTE_TYPE),
                                    indexedRichNote.first
                                )
                            ) { re, _ ->
                                if (re) finish() else toast(getString(R.string.failed_msg))
                            }
                        }
                        NoteEditSeen.NoteEditEvent.Finish -> finish()
                        NoteEditSeen.NoteEditEvent.PickImage -> startGalleyPick(true)?.let { re ->
                            noteEditSeen.insertImage(
                                RichPhotoStates(
                                    re.uri,
                                    null,
                                    re.name,
                                    re.date.toDate().toString()
                                )
                            )
                        }
                        NoteEditSeen.NoteEditEvent.PickVideo -> startGalleyPick(false)?.let { re ->
                            noteEditSeen.insertVideo(RichVideoStates(re.uri, null))
                        }
                        NoteEditSeen.NoteEditEvent.PickMusic -> startActivityForResult(
                            ActivityResultContracts.StartActivityForResult(),
                            AddMusic2BucketActivity::class.intent.apply {
                                putExtra(AddMusic2BucketSeen.MODE, AddMusic2BucketSeen.PICK_MUSIC)
                            }
                        )?.let { re ->
                            if (re.resultCode == RESULT_OK)
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
        re.data?.getStringExtra("GalleyData")?.toEntity(GalleyMedia::class.java)
    }

}