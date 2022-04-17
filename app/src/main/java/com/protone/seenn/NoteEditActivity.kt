package com.protone.seenn

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.toDate
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.note.entity.RichMusicStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.RichVideoStates
import com.protone.seen.GalleySeen
import com.protone.seen.NoteEditSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NoteEditActivity : BaseActivity<NoteEditSeen>() {
    override suspend fun main() {
        val noteEditSeen = NoteEditSeen(this)

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                noteEditSeen.viewEvent.onReceive {
                    when (it) {
                        NoteEditSeen.NoteEditEvent.Confirm -> {
                            val indexedRichNote = noteEditSeen.indexRichNote()
                        }
                        NoteEditSeen.NoteEditEvent.Finish -> finish()
                        NoteEditSeen.NoteEditEvent.PickImage -> startActivityForResult(
                            ActivityResultContracts.StartActivityForResult(),
                            GalleyActivity::class.intent.apply {
                                putExtra(
                                    GalleyActivity.CHOOSE_MODE,
                                    GalleySeen.CHOOSE_PHOTO
                                )
                            }
                        ).let { re ->
                            re?.data?.getStringExtra("GalleyData")
                                ?.toEntity(GalleyMedia::class.java)?.apply {
                                    noteEditSeen.insertImage(
                                        RichPhotoStates(uri, null, name, date.toDate().toString())
                                    )
                                }
                        }
                        NoteEditSeen.NoteEditEvent.PickVideo -> startActivityForResult(
                            ActivityResultContracts.StartActivityForResult(),
                            GalleyActivity::class.intent.apply {
                                putExtra(
                                    GalleyActivity.CHOOSE_MODE,
                                    GalleySeen.CHOOSE_VIDEO
                                )
                            }
                        ).let { re ->
                            re?.data?.getStringExtra("GalleyData")
                                ?.toEntity(GalleyMedia::class.java)?.apply {
                                    noteEditSeen.insertVideo(RichVideoStates(uri, null))
                                }
                        }
                        NoteEditSeen.NoteEditEvent.PickMusic -> {
                            noteEditSeen.insertMusic(RichMusicStates(Uri.EMPTY, null))
                        }
                    }

                }
            }

        }

    }
}