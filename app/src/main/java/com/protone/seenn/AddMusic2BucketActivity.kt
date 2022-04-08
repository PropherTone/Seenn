package com.protone.seenn

import android.content.Intent
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seen.AddMusic2BucketSeen
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AddMusic2BucketActivity : BaseActivity<AddMusic2BucketSeen>() {

    override suspend fun main() {
        val addMusic2BucketSeen = AddMusic2BucketSeen(this)

        setContentSeen(addMusic2BucketSeen)

        val bucket = intent.getStringExtra("BUCKET")

        if (bucket != null) {
            addMusic2BucketSeen.initSeen(bucket)
        } else {
            toast(getString(R.string.no_bucket))
            cancel()
        }

        while (isActive) {
            select<Unit> {
                event.onReceive{

                }
                addMusic2BucketSeen.viewEvent.onReceive{
                    when (it) {
                        AddMusic2BucketSeen.Event.Finished -> {
//                            workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                            finish()
                        }
                    }
                }
            }
        }
    }
}