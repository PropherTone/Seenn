package com.protone.seenn

import android.content.Intent
import com.protone.api.toBitmapByteArray
import com.protone.api.todayTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.AddBucketSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AddBucketActivity : BaseActivity<AddBucketSeen>() {

    companion object {
        @JvmStatic
        val BUCKET_ICON = "BUCKET_ICON"

        @JvmStatic
        val BUCKET_NAME = "BUCKET_NAME"

        @JvmStatic
        val BUCKET_DETAIL = "BUCKET_DETAIL"
    }

    override suspend fun main() {
        val addBucketSeen = AddBucketSeen(this)
        setContentSeen(addBucketSeen)

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                addBucketSeen.viewEvent.onReceive {
                    when (it) {
                        AddBucketSeen.Event.ChooseIcon -> {

                        }
                        AddBucketSeen.Event.Confirm -> {
                            addBucketSeen.addMusicBucket { re, name ->
                                Intent().apply {
                                    setResult(
                                        if (re) {
                                            putExtra(BUCKET_NAME, name)
                                            RESULT_OK
                                        } else RESULT_CANCELED, this
                                    )
                                }
                                finish()
                            }
                        }
                        AddBucketSeen.Event.Finished -> {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun AddBucketSeen.addMusicBucket(callBack: (result: Boolean, name: String) -> Unit) =
        DataBaseDAOHelper.addMusicBucketWithCallBack(
            MusicBucket(name, uri.toBitmapByteArray(), 0, detail, todayTime),
            callBack
        )

}