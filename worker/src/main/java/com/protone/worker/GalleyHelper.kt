package com.protone.worker

import com.protone.api.baseType.getFileName
import com.protone.api.context.SApplication
import com.protone.api.isInDebug
import com.protone.worker.media.scanAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object GalleyHelper : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    inline fun saveIconToLocal(
        fileName: String, byteArray: ByteArray?,
        crossinline callBack: (String?) -> Unit
    ) = launch(Dispatchers.IO) {
        var fileOutputStream: FileOutputStream? = null
        try {
            callBack.invoke(byteArray?.let {
                val tempPath =
                    "${SApplication.app.filesDir.absolutePath}/${fileName.getFileName()}.png"
                val file = File(tempPath)
                when {
                    file.exists() -> tempPath
                    file.createNewFile() -> {
                        fileOutputStream = FileOutputStream(file)
                        fileOutputStream?.write(it)
                        tempPath
                    }
                    else -> null
                }
            })
        } catch (e: IOException) {
            if (isInDebug()) e.printStackTrace()
            try {
                fileOutputStream?.flush()
                fileOutputStream?.close()
            } catch (e: IOException) {
            }
            callBack.invoke(null)
        }
    }

    fun updateAll(callBack: () -> Unit) = launch(Dispatchers.IO) {
        Medias.musicBucket[SApplication.app.getString(R.string.all_music)] = scanAudio { _, _ -> }
        callBack()
    }
}