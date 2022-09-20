package com.protone.worker.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.entity.GalleyBucket
import com.protone.api.entity.GalleyMedia
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.MediaAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleyFragmentViewModel : ViewModel() {

    private val _fragFlow = MutableSharedFlow<FragEvent>()
    val fragFlow get() = _fragFlow

    sealed class FragEvent {
        object SelectAll : FragEvent()
        object OnActionBtn : FragEvent()
        object IntoBox : FragEvent()

        data class AddBucket(val name: String, val list: MutableList<GalleyMedia>) : FragEvent()
        data class OnNewBucket(val pairs: Pair<Uri, Array<String>>) : FragEvent()

        data class OnSelect(val galleyMedia: MutableList<GalleyMedia>) : FragEvent()

        data class OnMediaDeleted(val galleyMedia: GalleyMedia) : FragEvent()
        data class OnMediaInserted(val galleyMedia: GalleyMedia) : FragEvent()
        data class OnMediaUpdated(val galleyMedia: GalleyMedia) : FragEvent()
    }

    var rightGalley: String = ""

    var isVideo: Boolean = false

    var isLock: Boolean = false
    var isBucketShowUp = true
    private var isDataSorted = false

    private val galleyMap = mutableMapOf<String?, MutableList<GalleyMedia>>()

    fun getGalley(galley: String) = galleyMap[galley]

    fun getGalleyName() = if (rightGalley == "") {
        R.string.all_galley.getString()
    } else rightGalley

    fun onTargetGalley(bucket: String): Boolean {
        return bucket == rightGalley || rightGalley == R.string.all_galley.getString()
    }

    fun sortData(combine: Boolean) = viewModelScope.launch(Dispatchers.Default) {
        galleyMap[R.string.all_galley.getString()] = mutableListOf()
        DatabaseHelper.instance
            .signedGalleyDAOBridge
            .run {
                observeGalley()
                val signedMedias =
                    (if (combine) getAllSignedMedia() else getAllMediaByType(isVideo)) as MutableList<GalleyMedia>?
                if (signedMedias == null) {
                    R.string.none.getString().toast()
                    return@launch
                }
                signedMedias.let {
                    galleyMap[R.string.all_galley.getString()] = it
                    sendEvent(
                        FragEvent.OnNewBucket(
                            Pair(
                                if (signedMedias.size > 0) signedMedias[0].uri else Uri.EMPTY,
                                arrayOf(
                                    R.string.all_galley.getString(),
                                    signedMedias.size.toString()
                                )
                            )
                        )
                    )
                }
                (if (combine) getAllGalley() else getAllGalley(isVideo))?.forEach {
                    galleyMap[it] =
                        ((if (combine) getAllMediaByGalley(it)
                        else getAllMediaByGalley(
                            it,
                            isVideo
                        )) as MutableList<GalleyMedia>).also { list ->
                            sendEvent(
                                FragEvent.OnNewBucket(
                                    Pair(
                                        if (list.size > 0) list[0].uri else Uri.EMPTY,
                                        arrayOf(it, list.size.toString())
                                    )
                                )
                            )
                        }
                }
                if (!isLock) sortPrivateData(signedMedias) else isDataSorted = true
            }
    }

    fun addBucket(name: String) {
        DatabaseHelper
            .instance
            .galleyBucketDAOBridge
            .insertGalleyBucketCB(GalleyBucket(name, isVideo)) { re, reName ->
                if (re) {
                    if (!isLock) {
                        sendEvent(
                            FragEvent.OnNewBucket((Pair(Uri.EMPTY, arrayOf(reName, "PRIVATE"))))
                        )
                        galleyMap[reName] = mutableListOf()
                    } else {
                        R.string.locked.getString().toast()
                    }
                } else {
                    R.string.failed_msg.getString().toast()
                }
            }
    }

    fun deleteGalleyBucket(bucket: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHelper.instance.galleyBucketDAOBridge.run {
                getGalleyBucket(bucket)?.let { deleteGalleyBucketAsync(it) }
            }
        }
    }

    private fun sortPrivateData(signedMedias: MutableList<GalleyMedia>) {
        viewModelScope.launch(Dispatchers.Default) {
            signedMedias.forEach {
                it.type?.forEach { type ->
                    if (galleyMap[type] == null) {
                        galleyMap[type] = mutableListOf()
                    }
                    galleyMap[type]?.add(it)
                }
            }
            (DatabaseHelper.instance.galleyBucketDAOBridge
                .getALLGalleyBucket(isVideo) as MutableList<GalleyBucket>?)?.forEach {
                sendEvent(FragEvent.OnNewBucket(Pair(Uri.EMPTY, arrayOf(it.type, "PRIVATE"))))
                galleyMap[it.type] = mutableListOf()
            }
            isDataSorted = true
        }
    }

    private fun observeGalley() {
        viewModelScope.launch(Dispatchers.Default) {
            val allGalley = R.string.all_galley.getString()
            suspend fun sortDeleteMedia(
                media: GalleyMedia,
                map: MutableMap<String?, MutableList<GalleyMedia>>,
                flow: MutableSharedFlow<FragEvent>
            ) {
                if (map[media.bucket]?.remove(media) == true
                    && (map[media.bucket]?.size ?: 0) <= 0
                ) {
                    map.remove(media.bucket)
                }
                map[allGalley]?.remove(media)
                flow.emit(FragEvent.OnMediaDeleted(media))
            }

            suspend fun insertNewMedia(
                map: MutableMap<String?, MutableList<GalleyMedia>>,
                media: GalleyMedia
            ) {
                if (map[media.bucket] == null) {
                    map[media.bucket] = mutableListOf<GalleyMedia>().also { it.add(media) }
                    FragEvent.OnNewBucket(
                        Pair(
                            media.uri,
                            arrayOf(media.bucket, map[media.bucket]?.size.toString())
                        )
                    ).let { sendEvent(it) }
                } else map[media.bucket]?.add(media)
            }

            DatabaseHelper.instance.mediaNotifier.buffer().collect {
                while (!isDataSorted) delay(200)
                when (it) {
                    is MediaAction.OnMediaDeleted -> {
                        if (it.media.isVideo != isVideo) return@collect
                        sortDeleteMedia(it.media, galleyMap, _fragFlow)
                    }
                    is MediaAction.OnMediaInserted -> {
                        if (it.media.isVideo != isVideo) return@collect
                        insertNewMedia(galleyMap, it.media)
                        galleyMap[allGalley]?.add(it.media)
                        sendEvent(FragEvent.OnMediaInserted(it.media))
                    }
                    is MediaAction.OnMediaUpdated -> {
                        if (it.media.isVideo != isVideo) return@collect
                        galleyMap[allGalley]?.first { sortMedia -> it.media.uri == sortMedia.uri }
                            ?.let { sortedMedia ->
                                val allIndex = galleyMap[allGalley]?.indexOf(sortedMedia)
                                if (allIndex != null && allIndex != -1) {
                                    galleyMap[allGalley]?.set(allIndex, it.media)
                                    val index = galleyMap[sortedMedia.bucket]?.indexOf(sortedMedia)
                                    if (sortedMedia.bucket != it.media.bucket) {
                                        galleyMap[sortedMedia.bucket]?.remove(sortedMedia)
                                        insertNewMedia(galleyMap, it.media)
                                        sendEvent(FragEvent.OnMediaInserted(it.media))
                                        return@let
                                    } else if (index != null && index != -1) {
                                        galleyMap[sortedMedia.bucket]?.set(index, sortedMedia)
                                    }
                                    sendEvent(FragEvent.OnMediaUpdated(it.media))
                                }
                            }
                    }
                    else -> Unit
                }
            }
        }
    }

    suspend fun sendEvent(fragEvent: FragEvent) {
        _fragFlow.emit(fragEvent)
    }

    suspend fun insertNewMedias(galley: String, list: MutableList<GalleyMedia>) =
        withContext(Dispatchers.Default) {
            if (!isLock) {
                if (galleyMap[galley] == null) {
                    galleyMap[galley] = mutableListOf()
                }
                galleyMap[galley]?.addAll(list)
            }
        }

}