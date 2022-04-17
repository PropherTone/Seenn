package com.protone.seenn

import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toUri
import com.protone.api.toBitmapByteArray
import com.protone.api.toMediaBitmapByteArray
import com.protone.api.todayTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.AddBucketSeen
import com.protone.seen.GalleySeen
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
                            startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                GalleyActivity::class.intent.also { intent ->
                                    intent.putExtra(
                                        GalleyActivity.CHOOSE_MODE,
                                        GalleySeen.CHOOSE_PHOTO
                                    )
                                }
                            )?.let { result ->
                                addBucketSeen.uri = result.data?.getStringExtra("Uri")?.toUri()
                            }
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

    private inline fun AddBucketSeen.addMusicBucket(crossinline callBack: (result: Boolean, name: String) -> Unit) =
        DataBaseDAOHelper.addMusicBucketWithCallBack(
            MusicBucket(
                name,
                if (uri != null) uri?.toMediaBitmapByteArray() else null,
                0,
                detail,
                todayTime
            ),
            callBack
        )

}