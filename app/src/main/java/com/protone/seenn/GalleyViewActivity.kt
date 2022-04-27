package com.protone.seenn

import android.net.Uri
import android.util.Log
import com.protone.api.context.intent
import com.protone.api.context.onUiThread
import com.protone.api.json.toEntity
import com.protone.api.toDate
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
import java.util.stream.Collectors

class GalleyViewActivity : BaseActivity<GalleyViewSeen>() {

    companion object {
        const val MEDIA = "MediaData"
        const val TYPE = "MediaType"
    }

    private var curPosition: Int = 0
    private lateinit var galleyMedias: MutableList<GalleyMedia>

    override suspend fun main() {
        val galleyViewSeen = GalleyViewSeen(this)
        setContentSeen(galleyViewSeen)

        galleyMedias = (if (intent.getBooleanExtra(TYPE, false))
            Galley.video[getString(R.string.all_galley)] ?: mutableListOf()
        else Galley.photo[getString(R.string.all_galley)]) ?: mutableListOf()

        galleyViewSeen.init()

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                galleyViewSeen.viewEvent.onReceive {
                    when (it) {
                        GalleyViewSeen.GalleyVEvent.Finish -> finish()
                        GalleyViewSeen.GalleyVEvent.ShowAction -> {}
                        GalleyViewSeen.GalleyVEvent.SetNotes -> {
                            DataBaseDAOHelper.getSignedMediaCB(
                                galleyMedias[curPosition].uri
                            ) { galleyMedia ->
                                onUiThread {
                                    galleyViewSeen.setNotes(
                                        (galleyMedia?.notes ?: mutableListOf())
                                                as MutableList<String>
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun GalleyViewSeen.init() {
        initList {
            startActivity(NoteViewActivity::class.intent.apply {
                putExtra(NoteViewActivity.NOTE_NAME, it)
            })
        }
        initViewPager(getMediaIndex(), galleyMedias) { position ->
            curPosition = position
            galleyMedias[position].let { m ->
                setMediaInfo(
                    m.name,
                    m.date.toDate().toString(),
                    "${m.size / 1024}Kb",
                    m.cate?.toSplitString(",") ?: "",
                    m.type?.toSplitString(",") ?: ""
                )
            }
        }
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