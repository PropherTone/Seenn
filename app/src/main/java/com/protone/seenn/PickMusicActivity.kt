package com.protone.seenn

import android.content.Intent
import com.protone.api.SearchModel
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.database.room.entity.Music
import com.protone.seen.PickMusicSeen
import com.protone.seenn.broadcast.workLocalBroadCast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class PickMusicActivity : BaseActivity<PickMusicSeen>() {

    private var mode: String? = null

    private val data : MutableList<Music> = mutableListOf()
    override suspend fun main() {
        val pickMusicSeen = PickMusicSeen(this)
        setContentSeen(pickMusicSeen)

        mode = intent.getStringExtra(PickMusicSeen.MODE)

        val bucket = when (mode) {
            PickMusicSeen.PICK_MUSIC -> getString(R.string.all_music)
            else -> intent.getStringExtra(PickMusicSeen.BUCKET_NAME)
        }

        if (bucket != null) {
            pickMusicSeen.initSeen(bucket, mode ?: PickMusicSeen.ADD_BUCKET)
        } else {
            toast(getString(R.string.no_bucket))
            cancel()
        }

        pickMusicSeen.getList()?.let { data.addAll(it) }
        val searchModel = SearchModel(pickMusicSeen.getQueryInput()) {
            pickMusicSeen.offer(PickMusicSeen.Event.Query)
        }
        while (isActive) {
            select<Unit> {
                event.onReceive {}
                pickMusicSeen.viewEvent.onReceive {
                    when (it) {
                        PickMusicSeen.Event.Finished -> {
                            workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                            finish()
                        }
                        PickMusicSeen.Event.Confirm -> {
                            val selectList = pickMusicSeen.getSelectList()
                            if (selectList != null && selectList.size > 0) {
                                setResult(RESULT_OK, Intent().apply {
                                    data = selectList[0].uri
                                })
                            } else toast(getString(R.string.cancel))
                            cancel()
                        }
                        PickMusicSeen.Event.Query -> pickMusicSeen.query(searchModel.getInput())
                    }
                }
            }
        }

    }

    private suspend fun PickMusicSeen.query(input: String) {
        if (input.isEmpty()) return
        withContext(Dispatchers.IO) {
            refreshList(data.asFlow().filter {
                it.displayName?.contains(input,true) == true || it.album?.contains(input,true) == true
            }.buffer().toList() as MutableList<Music>)
        }
    }
}