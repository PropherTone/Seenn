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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GalleyFragmentViewModel : ViewModel() {

    private val _fragFlow = MutableSharedFlow<FragEvent>()
    val fragFlow get() = _fragFlow

    sealed class FragEvent {
        object SelectAll : FragEvent()
        object OnActionBtn : FragEvent()
        object IntoBox : FragEvent()
        object OnGetAllGalley : FragEvent()

        data class AddBucket(val name: String, val list: MutableList<GalleyMedia>) : FragEvent()
        data class OnNewBucket(val pairs: Pair<Uri, Array<String>>) : FragEvent()
        data class OnListUpdate(val data: MutableList<GalleyMedia>?) : FragEvent()
        data class OnSelect(val galleyMedia: MutableList<GalleyMedia>) : FragEvent()

        data class OnMediaDeleted(val galleyMedia: GalleyMedia) : FragEvent()
        data class OnMediaInserted(val galleyMedia: GalleyMedia) : FragEvent()
        data class OnMediaUpdated(val galleyMedia: GalleyMedia) : FragEvent()
    }

    lateinit var rightGalley: String

    var isVideo: Boolean = false
    var isLock: Boolean = false
    var combine: Boolean = false

    var isBucketShowUp = true

    val galleyMap = mutableMapOf<String?, MutableList<GalleyMedia>>()
    @Synchronized get

    fun sendEvent(fragEvent: FragEvent) {
        viewModelScope.launch {
            _fragFlow.emit(fragEvent)
        }
    }

    fun sortPrivateData() = viewModelScope.launch(Dispatchers.IO) {
        (DatabaseHelper.instance.galleyBucketDAOBridge
            .getALLGalleyBucket(isVideo) as MutableList<GalleyBucket>?)?.forEach {
            sendEvent(FragEvent.OnNewBucket(Pair(Uri.EMPTY, arrayOf(it.type, "PRIVATE"))))
            galleyMap[it.type] = mutableListOf()
        }
    }

    fun sortData() = viewModelScope.launch(Dispatchers.Default) {
        galleyMap[R.string.all_galley.getString()] = mutableListOf()
        DatabaseHelper
            .instance
            .signedGalleyDAOBridge
            .run {
                val signedMedias =
                    (if (combine) getAllSignedMediaRs() else getAllMediaByTypeRs(isVideo)) as MutableList<GalleyMedia>?
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
                    sendEvent(FragEvent.OnGetAllGalley)
                }
                if (!isLock) launch(Dispatchers.Default) {
                    signedMedias.forEach {
                        it.type?.forEach { type ->
                            if (galleyMap[type] == null) {
                                galleyMap[type] = mutableListOf()
                            }
                            galleyMap[type]?.add(it)
                        }
                    }
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
            }
    }

    fun observeGalley() {
        val allGalley = R.string.all_galley.getString()
        viewModelScope.launch(Dispatchers.Default) {
            suspend fun sortDeleteMedia(
                media: GalleyMedia,
                map: MutableMap<String?, MutableList<GalleyMedia>>,
                flow: MutableSharedFlow<FragEvent>
            ) {
                map[media.bucket]?.remove(media)
                map[allGalley]?.remove(media)
                flow.emit(FragEvent.OnMediaDeleted(media))
            }
            DatabaseHelper.instance.mediaNotifier.buffer().collect {
                when (it) {
                    is MediaAction.OnMediaByUriDeleted ->
                        sortDeleteMedia(it.media, galleyMap, _fragFlow)
                    is MediaAction.OnMediaDeleted ->
                        sortDeleteMedia(it.media, galleyMap, _fragFlow)
                    is MediaAction.OnMediaInserted -> {
                        insertNewMedia(it.media)
                        galleyMap[allGalley]?.add(0, it.media)
                        _fragFlow.emit(FragEvent.OnMediaInserted(it.media))
                    }
                    is MediaAction.OnMediaUpdated -> {
                        galleyMap[allGalley]?.first { sortMedia -> it.media.uri == sortMedia.uri }
                            ?.let { sortedMedia ->
                                val allIndex = galleyMap[allGalley]?.indexOf(sortedMedia)
                                if (allIndex != null && allIndex != -1) {
                                    galleyMap[allGalley]?.set(allIndex, it.media)
                                    val index = galleyMap[sortedMedia.bucket]?.indexOf(sortedMedia)
                                    if (sortedMedia.bucket != it.media.bucket) {
                                        galleyMap[sortedMedia.bucket]?.remove(sortedMedia)
                                        insertNewMedia(it.media)
                                        _fragFlow.emit(FragEvent.OnMediaInserted(it.media))
                                        return@let
                                    } else if (index != null && index != -1) {
                                        galleyMap[sortedMedia.bucket]?.set(index, sortedMedia)
                                    }
                                    _fragFlow.emit(FragEvent.OnMediaUpdated(it.media))
                                }
                            }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onTargetGalley(bucket: String): Boolean {
        return bucket == rightGalley || rightGalley == R.string.all_galley.getString()
    }

    suspend fun insertNewMedia(media: GalleyMedia) {
        if (galleyMap[media.bucket] == null) {
            galleyMap[media.bucket] = mutableListOf()
            FragEvent.OnNewBucket(
                Pair(
                    media.uri,
                    arrayOf(media.bucket, galleyMap[media.bucket]?.size.toString())
                )
            ).let { _fragFlow.emit(it) }
        }
        galleyMap[media.bucket]?.add(0, media)
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

}