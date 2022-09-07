package com.protone.worker.viewModel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.protone.api.context.SApplication
import com.protone.api.img.Blur
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel : BaseViewModel() {
    var btnY = 0f
    val btnH = SApplication.app.resources.getDimensionPixelSize(R.dimen.action_icon_p)

    sealed class MainViewEvent {
        object Galley : ViewEvent
        object Music : ViewEvent
        object Note : ViewEvent
        object UserConfig : ViewEvent
    }

    suspend fun getMusics(bucketName: String) = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.run {
            musicBucketDAOBridge.getMusicBucketByName(bucketName)?.musicBucketId?.let {
                musicWithMusicBucketDAOBridge.getMusicWithMusicBucket(it)
            }
        }
    }

    fun loadBlurIcon(path: String): Bitmap? {
        return try {
            Blur(SApplication.app).blur(
                BitmapFactory.decodeFile(path),
                radius = 10,
                sampling = 10
            )
        } catch (e: Exception) {
            null
        }
    }
}