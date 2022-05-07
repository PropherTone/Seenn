package com.protone.seenn

import android.content.Intent
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seen.PickMusicActivity
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class PickMusicActivity : BaseActivity<PickMusicActivity>() {

    private var mode: String? = null

    override suspend fun main() {
        val pickMusicActivity = PickMusicActivity(this)

        setContentSeen(pickMusicActivity)

        mode = intent.getStringExtra(PickMusicActivity.MODE)

        val bucket = when (mode) {
            PickMusicActivity.PICK_MUSIC -> getString(R.string.all_music)
            else -> intent.getStringExtra(PickMusicActivity.BUCKET_NAME)
        }

        if (bucket != null) {
            pickMusicActivity.initSeen(bucket, mode ?: PickMusicActivity.ADD_BUCKET)
        } else {
            toast(getString(R.string.no_bucket))
            cancel()
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                pickMusicActivity.viewEvent.onReceive {
                    when (it) {
                        PickMusicActivity.Event.Finished -> {
                            workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                            finish()
                        }
                        PickMusicActivity.Event.Confirm -> {
                            val selectList = pickMusicActivity.getSelectList()
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