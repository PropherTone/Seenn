package com.protone.seenn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.protone.seen.AddBucketSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AddBucketActivity : BaseActivity<AddBucketSeen>() {

    override suspend fun main() {
        val addBucketSeen = AddBucketSeen(this)
        setContentSeen(addBucketSeen)

        while (isActive) {
            select<Unit> {
                event.onReceive{

                }
                addBucketSeen.viewEvent.onReceive{
                    when (it) {
                        AddBucketSeen.Event.Finished -> {
                            val intent = Intent().apply {
                                putExtra("BUCKET_NAME",addBucketSeen.binding.edit.text.toString())
                            }
                            setResult(RESULT_OK,intent)
                            finish()
                        }
                    }
                }
            }
        }
    }

}