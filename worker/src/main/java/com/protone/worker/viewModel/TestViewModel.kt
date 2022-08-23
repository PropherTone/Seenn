package com.protone.worker.viewModel

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import com.protone.api.TAG
import com.protone.api.entity.GalleyMedia
import com.protone.worker.Medias
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.media.isUriExist
import com.protone.worker.media.scanGalleyWithUri
import com.protone.worker.media.scanPicture
import com.protone.worker.media.scanVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.streams.toList

class TestViewModel : BaseViewModel() {

    fun updateGalley(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runningTime("updateGalley(uri: Uri) ") {
            scanGalleyWithUri(uri) {
                val checkedMedia =
                    DatabaseHelper
                        .instance
                        .signedGalleyDAOBridge
                        .insertSignedMediaChecked(it)
                if (checkedMedia != null) {
                    log("updateGalley(uri: Uri): 相册更新完毕")
                }
            }
        }
    }

    fun filterGalley() = viewModelScope.launch(Dispatchers.IO) {
        fun sortMedia(allSignedMedia: MutableList<GalleyMedia>, galleyMedia: GalleyMedia) {
            allSignedMedia.stream().filter { it.uri == galleyMedia.uri }
                .toList().let { list ->
                    if (list.isNotEmpty()) allSignedMedia.remove(list[0])
                }
        }

        val updatedMedia = arrayListOf<GalleyMedia>()
        runningTime("filterGalley()") {
            val allSignedMedia =
                DatabaseHelper.instance.signedGalleyDAOBridge.getAllSignedMedia() as MutableList
            flow {
                scanPicture { _, galleyMedia ->
                    emit(galleyMedia)
                }
                scanVideo { _, galleyMedia ->
                    emit(galleyMedia)
                }
            }.buffer().collect {
//                val checkedMedia =
//                    DatabaseHelper.instance.signedGalleyDAOBridge.insertSignedMediaChecked(it)
//                sortMedia(allSignedMedia, it)
//                if (checkedMedia != null && !updatedMedia.contains(checkedMedia)) {
//                    updatedMedia.add(checkedMedia)
//                }
            }
        }
    }

    fun updateGalleyNew() = DatabaseHelper.instance.signedGalleyDAOBridge.run {
        viewModelScope.launch(Dispatchers.IO) {
            runningTime("updateGalleyNew()") {
                val allMedia = getAllMediaByType(false) as MutableList<GalleyMedia>

            }
        }
    }

    fun updateGalley() = DatabaseHelper.instance.signedGalleyDAOBridge.run {
        viewModelScope.launch(Dispatchers.IO) {
            runningTime("updateGalley()") {

                val sortMedias = async(Dispatchers.IO) {
                    val allSignedMedia = getAllSignedMedia() as MutableList
                    flow {
                        allSignedMedia.forEach {
                            if (!isUriExist(it.uri)) {
                                emit(it)
                            }
                        }
                    }.buffer().collect {
                        deleteSignedMedia(it)
                        it.mediaStatus = GalleyMedia.MediaStatus.Deleted
                        Medias.galleyNotifier.emit(it)
                    }
                }

                val scanPicture = async(Dispatchers.IO) {
                    flow {
                        scanPicture { _, galleyMedia ->
                            emit(galleyMedia)
                        }
                    }.buffer().collect {
                        //主要耗时
                        val checkedMedia = insertSignedMediaChecked(it)
                        if (checkedMedia != null) {
                            Medias.galleyNotifier.emit(it)
                        }
                    }
                }

                val scanVideo = async(Dispatchers.IO) {
                    flow {
                        scanVideo { _, galleyMedia ->
                            emit(galleyMedia)
                        }
                    }.buffer().collect {
                        val checkedMedia = insertSignedMediaChecked(it)
                        if (checkedMedia != null) {
                            Medias.galleyNotifier.emit(it)
                        }
                    }
                }

                sortMedias.await()
                scanPicture.await()
                scanVideo.await()
            }
        }
    }

    fun mediasCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val allGalley = DatabaseHelper.instance.signedGalleyDAOBridge.getAllSignedMedia()
            log("galley size :${allGalley?.size}")
        }
    }

    @SuppressLint("StaticFieldLeak")
    private lateinit var logText: TextView
    val stringBuilder = StringBuilder()

    fun setLogView(textView: TextView) {
        logText = textView
    }

    fun log(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            stringBuilder.append("$msg\n")
            logText.text = stringBuilder.toString()
            Log.d(TAG, msg)
        }
    }

    private suspend fun runningTime(name: String, block: suspend () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log("$name start running")
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log("$name running time: ${System.currentTimeMillis() - start}\n")
    }

    private fun runningTimeNoSuspend(block: () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log("function start running")
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log("function running time: ${System.currentTimeMillis() - start}\n")
    }
}