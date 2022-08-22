package com.protone.seenn.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protone.api.TAG
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.entity.GalleyBucket
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class GalleyFragmentViewModel : ViewModel() {

    private val _fragFlow = MutableSharedFlow<FragEvent>()
    val fragFlow get() = _fragFlow

    sealed class FragEvent {
        object SelectAll : FragEvent()
        object OnActionBtn : FragEvent()
        object IntoBox : FragEvent()
        object OnGetAllGalley : FragEvent()
        data class DeleteMedia(val galleyMedia: GalleyMedia) : FragEvent()
        data class AddBucket(val name: String, val list: MutableList<GalleyMedia>) : FragEvent()
        data class OnGalleyUpdate(val media: GalleyMedia) : FragEvent()
        data class OnNewBucket(val pairs: Pair<Uri, Array<String>>) : FragEvent()
        data class OnListUpdate(val data: MutableList<GalleyMedia>?) : FragEvent()
        data class OnSelect(val galleyMedia: MutableList<GalleyMedia>) : FragEvent()
    }

    lateinit var rightGalley: String

    var isVideo: Boolean = false
    var isLock: Boolean = false
    var combine: Boolean = false

    var isBucketShowUp = true

    val galleyMap = mutableMapOf<String?, MutableList<GalleyMedia>>()

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

    inline fun updateGalley(
        media: GalleyMedia,
        crossinline callBack: (GalleyMedia.MediaStatus, GalleyMedia) -> Unit
    ) = viewModelScope.launch(Dispatchers.Default) {
        val allGalley = R.string.all_galley.getString()
        if (galleyMap[allGalley]?.contains(media) == false && (isVideo == media.isVideo || combine)) when (media.mediaStatus) {
            GalleyMedia.MediaStatus.Updated -> {
                galleyMap[allGalley]?.first { media -> media.uri == media.uri }?.let { media ->
                    val allIndex = galleyMap[allGalley]?.indexOf(media)
                    if (allIndex != null && allIndex != -1) {
                        galleyMap[allGalley]?.set(allIndex, media)
                        val index = galleyMap[media.bucket]?.indexOf(media)
                        if (media.bucket != media.bucket) {
                            galleyMap[media.bucket]?.remove(media)
                            insertNewMedia(media)
                            callBack.invoke(GalleyMedia.MediaStatus.NewInsert, media)
                            return@let
                        } else if (index != null && index != -1) {
                            galleyMap[media.bucket]?.set(index, media)
                        }
                    }
                    callBack.invoke(GalleyMedia.MediaStatus.Updated, media)
                }
            }
            GalleyMedia.MediaStatus.Deleted -> {
                galleyMap[media.bucket]?.remove(media)
                galleyMap[allGalley]?.remove(media)
                callBack.invoke(GalleyMedia.MediaStatus.Deleted, media)
            }
            GalleyMedia.MediaStatus.NewInsert -> {
                insertNewMedia(media)
                galleyMap[allGalley]?.add(0, media)
                callBack.invoke(GalleyMedia.MediaStatus.NewInsert, media)
            }
        }
        Log.d(TAG, "GalleyFragment updateGalley done")
    }

    fun onTargetGalley(bucket: String): Boolean {
        return bucket == rightGalley || rightGalley == R.string.all_galley.getString()
    }

    suspend fun insertNewMedia(it: GalleyMedia) {
        if (galleyMap[it.bucket] == null) {
            galleyMap[it.bucket] = mutableListOf()
            FragEvent.OnNewBucket(
                Pair(
                    it.uri,
                    arrayOf(it.bucket, galleyMap[it.bucket]?.size.toString())
                )
            ).apply { _fragFlow.emit(this) }
        }
        galleyMap[it.bucket]?.add(0, it)
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