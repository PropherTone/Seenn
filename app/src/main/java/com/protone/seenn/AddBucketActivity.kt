package com.protone.seenn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.protone.seen.AddBucketSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AddBucketActivity : BaseActivity<AddBucketSeen>() {

    companion object{
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
                event.onReceive{

                }
                addBucketSeen.viewEvent.onReceive{
                    when (it) {
                        AddBucketSeen.Event.ChooseIcon->{

                        }
                        AddBucketSeen.Event.Confirm -> {
                            val intent = Intent().apply {
                                putExtra(BUCKET_ICON,addBucketSeen.binding.musicBucketEnterName.text.toString())
                                putExtra(BUCKET_NAME,addBucketSeen.binding.musicBucketEnterName.text.toString())
                                putExtra(BUCKET_DETAIL,addBucketSeen.binding.musicBucketEnterDetial.text.toString())
                            }
                            setResult(RESULT_OK,intent)
                            finish()
                        }
                        AddBucketSeen.Event.Finished->{
                            finish()
                        }
                    }
                }
            }
        }
    }

}