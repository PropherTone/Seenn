package com.protone.seenn

import android.content.Intent
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.seen.PickMusicSeen
import com.protone.seenn.broadcast.workLocalBroadCast
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class PickMusicActivity : BaseActivity<PickMusicSeen>() {

    private var mode: String? = null

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
                    }
                }
            }
        }

    }

}