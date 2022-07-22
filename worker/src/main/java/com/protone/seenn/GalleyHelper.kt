package com.protone.seenn

import com.protone.api.context.SApplication
import com.protone.api.baseType.getFileName
import com.protone.api.isInDebug
import com.protone.seenn.media.scanAudio
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object GalleyHelper : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    inline fun saveIconToLocal(
        fileName: String, byteArray: ByteArray?,
        crossinline callBack: (String?) -> Unit
    ) = launch {
        suspendCancellableCoroutine<String?> {
            var fileOutputStream: FileOutputStream? = null
            try {
                byteArray?.let {
                    val tempPath =
                        "${SApplication.app.filesDir.absolutePath}/${fileName.getFileName()}.jpg"
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
                if (isInDebug()) e.printStackTrace()
                try {
                    fileOutputStream?.flush()
                    fileOutputStream?.close()
                } catch (e: IOException) {
                }
                callBack.invoke(null)
            }
            this.cancel()
        }
    }

    fun updateAll(callBack: () -> Unit) = launch {
        Medias.musicBucket[SApplication.app.getString(R.string.all_music)] = scanAudio { _, _ -> }
        callBack()
        cancel()
    }
}