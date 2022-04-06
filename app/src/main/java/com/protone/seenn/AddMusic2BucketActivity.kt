package com.protone.seenn

import com.protone.seen.AddMusic2BucketSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AddMusic2BucketActivity : BaseActivity<AddMusic2BucketSeen>() {

    override suspend fun main() {
        val addMusic2BucketSeen = AddMusic2BucketSeen(this)

        setContentSeen(addMusic2BucketSeen)

        addMusic2BucketSeen.initSeen()

        while (isActive) {
            select<Unit> {
                event.onReceive{

                }
                addMusic2BucketSeen.viewEvent.onReceive{
                    when (it) {
                        AddMusic2BucketSeen.Event.Finished-> finish()
                    }
                }
            }
        }
    }
}