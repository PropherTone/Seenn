package com.protone.mediamodle

import com.protone.api.context.Global
import com.protone.api.getFileName
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.mediamodle.media.scanAudio
import com.protone.mediamodle.media.scanPicture
import com.protone.mediamodle.media.scanVideo
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

object GalleyHelper : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val threadPool = Executors.newCachedThreadPool()

    inline fun saveIconToLocal(
        fileName: String, byteArray: ByteArray?,
        crossinline callBack: (String?) -> Unit
    ) = launch {
        suspendCancellableCoroutine<String?> {
            var fileOutputStream: FileOutputStream? = null
            try {
                byteArray?.let {
                    val tempPath = "${Global.application.filesDir.absolutePath}/${fileName.getFileName()}.jpg"
                    val file = File(tempPath)
                    callBack.invoke(
                        when {
                            file.exists() -> tempPath
                            file.createNewFile() -> {
                                fileOutputStream = FileOutputStream(file)
                                fileOutputStream?.write(it)
                                tempPath
                            }
                            else -> null
                        }
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
                try {
                    fileOutputStream?.flush()
                    fileOutputStream?.close()
                } catch (e: IOException) {}
                callBack.invoke(null)
            }
            this.cancel()
        }
    }

    private suspend fun updateMusicCO() = withContext(Dispatchers.IO) {
        Galley.musicBucket[Global.application.getString(R.string.all_music)] = scanAudio()
        cancel()
    }

    fun updateAll(callBack: () -> Unit) = threadPool.execute {
        val job3 = launch { updateMusicCO() }
        while (job3.isActive) continue
        callBack()
    }

}