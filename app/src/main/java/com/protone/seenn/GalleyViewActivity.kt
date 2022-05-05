package com.protone.seenn

import com.protone.api.context.intent
import com.protone.api.context.onUiThread
import com.protone.api.json.toEntity
import com.protone.api.toDateString
import com.protone.api.toSplitString
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.Galley
import com.protone.seen.GalleyViewSeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class GalleyViewActivity : BaseActivity<GalleyViewSeen>() {

    companion object {
        const val MEDIA = "GalleyViewActivity:MediaData"
        const val TYPE = "GalleyViewActivity:IsVideo"
    }

    private var curPosition: Int = 0
    private lateinit var galleyMedias: MutableList<GalleyMedia>

    override suspend fun main() {
        val galleyViewSeen = GalleyViewSeen(this)
        setContentSeen(galleyViewSeen)

        val isVideo = intent.getBooleanExtra(TYPE, false)
        galleyMedias = (if (isVideo)
            Galley.video[getString(R.string.all_galley)] ?: mutableListOf()
        else Galley.photo[getString(R.string.all_galley)]) ?: mutableListOf()

        galleyViewSeen.init(isVideo)

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                galleyViewSeen.viewEvent.onReceive {
                    when (it) {
                        GalleyViewSeen.GalleyVEvent.Finish -> finish()
                        GalleyViewSeen.GalleyVEvent.ShowAction -> galleyViewSeen.showPop()
                        GalleyViewSeen.GalleyVEvent.SetNotes -> galleyViewSeen.setNotes()
                        GalleyViewSeen.GalleyVEvent.AddCato -> {}
                        GalleyViewSeen.GalleyVEvent.Delete -> {}
                        GalleyViewSeen.GalleyVEvent.IntoBox -> {}
                        GalleyViewSeen.GalleyVEvent.MoveTo -> {}
                        GalleyViewSeen.GalleyVEvent.Rename -> {}
                        GalleyViewSeen.GalleyVEvent.SelectAll -> {}
                    }
                }
            }
        }
    }

    private suspend fun GalleyViewSeen.init(isVideo: Boolean) {
        initList {
            startActivity(NoteViewActivity::class.intent.apply {
                putExtra(NoteViewActivity.NOTE_NAME, it)
            })
        }
        val mediaIndex = getMediaIndex()
        initViewPager(mediaIndex, galleyMedias, isVideo) { position ->
            curPosition = position
            setMediaInfo(position)
        }
        setMediaInfo(mediaIndex)
        setNotes()
    }

    private fun GalleyViewSeen.setNotes() {
        DataBaseDAOHelper.getSignedMediaCB(
            galleyMedias[curPosition].uri
        ) { galleyMedia ->
            onUiThread {
                ((galleyMedia?.notes
                    ?: mutableListOf()) as MutableList<String>).let { setNotes(it) }
            }
        }
    }

    private fun GalleyViewSeen.setMediaInfo(position: Int) = galleyMedias[position].let { m ->
        setMediaInfo(
            m.name,
            m.date.toDateString().toString(),
            "${m.size / 1024}Kb",
            m.cate?.toSplitString(",") ?: "",
            m.type?.toSplitString(",") ?: ""
        )
    }

    private suspend fun getMediaIndex() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Int> { co ->
            val galleyMedia =
                intent.getStringExtra(MEDIA)?.toEntity(GalleyMedia::class.java)
            val indexOf = galleyMedias.indexOf(galleyMedia)
            curPosition = indexOf
            co.resumeWith(Result.success(indexOf))
        }
    }

}