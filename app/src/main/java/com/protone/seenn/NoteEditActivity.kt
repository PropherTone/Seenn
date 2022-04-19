package com.protone.seenn

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.toBitmapByteArray
import com.protone.api.toDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Note
import com.protone.mediamodle.GalleyHelper
import com.protone.mediamodle.note.entity.RichMusicStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.RichVideoStates
import com.protone.seen.GalleySeen
import com.protone.seen.NoteEditSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NoteEditActivity : BaseActivity<NoteEditSeen>() {

    companion object {
        const val NOTE_TYPE = "NoteType"
    }

    override suspend fun main() {
        val noteEditSeen = NoteEditSeen(this)

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                noteEditSeen.viewEvent.onReceive {
                    when (it) {
                        NoteEditSeen.NoteEditEvent.Confirm -> {
                            DataBaseDAOHelper.insertNoteCB(Note()) {
                                finish()
                            }
                            val indexedRichNote = noteEditSeen.indexRichNote()
                        }
                        NoteEditSeen.NoteEditEvent.Finish -> finish()
                        NoteEditSeen.NoteEditEvent.PickImage -> startGalleyPick(true).let { re ->
                            re?.data?.getStringExtra("GalleyData")
                                ?.toEntity(GalleyMedia::class.java)?.apply {
                                    noteEditSeen.insertImage(
                                        RichPhotoStates(uri, null, name, date.toDate().toString())
                                    )
                                }
                        }
                        NoteEditSeen.NoteEditEvent.PickVideo -> startGalleyPick(false).let { re ->
                            re?.data?.getStringExtra("GalleyData")
                                ?.toEntity(GalleyMedia::class.java)?.apply {
                                    noteEditSeen.insertVideo(RichVideoStates(uri, null))
                                }
                        }
                        NoteEditSeen.NoteEditEvent.PickMusic -> {
                            noteEditSeen.insertMusic(RichMusicStates(Uri.EMPTY, null))
                        }
                        NoteEditSeen.NoteEditEvent.PickIcon -> startGalleyPick(true).let { re ->
                            re?.data?.getStringExtra("GalleyData")
                                ?.toEntity(GalleyMedia::class.java)?.apply {
//                                    GalleyHelper.saveIconToLocal()
                                    this.uri.toBitmapByteArray()
                                }
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
        }
    )

}