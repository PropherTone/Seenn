package com.protone.seenn

import android.content.Intent
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.mediamodle.GalleyHelper
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seen.AddMusic2BucketSeen
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AddMusic2BucketActivity : BaseActivity<AddMusic2BucketSeen>() {

    private var mode: String? = null

    override suspend fun main() {
        val addMusic2BucketSeen = AddMusic2BucketSeen(this)

        setContentSeen(addMusic2BucketSeen)

        mode = intent.getStringExtra(AddMusic2BucketSeen.MODE)

        val bucket = when (mode) {
            AddMusic2BucketSeen.PICK_MUSIC -> getString(R.string.all_music)
            else -> intent.getStringExtra(AddMusic2BucketSeen.BUCKET_NAME)
        }

        if (bucket != null) {
            addMusic2BucketSeen.initSeen(bucket, mode ?: AddMusic2BucketSeen.ADD_BUCKET)
        } else {
            toast(getString(R.string.no_bucket))
            cancel()
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                addMusic2BucketSeen.viewEvent.onReceive {
                    when (it) {
                        AddMusic2BucketSeen.Event.Finished -> {
                            workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                            setResult(RESULT_OK)
                            finish()
                        }
                        AddMusic2BucketSeen.Event.Confirm -> {
                            val selectList = addMusic2BucketSeen.getSelectList()
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