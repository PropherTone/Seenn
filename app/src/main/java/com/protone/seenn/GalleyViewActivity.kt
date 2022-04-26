package com.protone.seenn

import android.util.Log
import com.protone.api.context.intent
import com.protone.api.json.toEntity
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
        const val MEDIA = "MediaData"
        const val TYPE = "MediaType"
    }

    override suspend fun main() {
        val galleyViewSeen = GalleyViewSeen(this)
        setContentSeen(galleyViewSeen)

        galleyViewSeen.init()

        while (isActive) {
            select<Unit> {
                event.onReceive {}

            }
        }
    }

    private suspend fun GalleyViewSeen.init() {
        initList {
            startActivity(NoteViewActivity::class.intent.apply {
                putExtra(NoteViewActivity.NOTE_NAME, it)
            })
        }
        (if (intent.getBooleanExtra(TYPE, false))
            Galley.video[getString(R.string.all_galley)]
        else Galley.photo[getString(R.string.all_galley)])?.let {
            initViewPager(withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { co ->
                    val galleyMedia =
                        intent.getStringExtra(MEDIA)?.toEntity(GalleyMedia::class.java)
                    val indexOf = Galley.allPhoto?.indexOf(galleyMedia)
                    co.resumeWith(Result.success(indexOf ?: 0))
                }
            }, it) {

            }
        }
    }

}