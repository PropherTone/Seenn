package com.protone.seenn.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.entity.GalleyBucket
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class GalleyFragmentViewModel : ViewModel() {

    companion object {
        const val OnUpdated = "Media_Updated"
        const val OnDeleted = "Media_Deleted"
        const val OnNew = "New_Media"
    }


    private val _fragFlow = MutableSharedFlow<FragEvent>()
    val fragFlow get() = _fragFlow

    sealed class FragEvent {
        object SelectAll : FragEvent()
        object OnActionBtn : FragEvent()
        object IntoBox : FragEvent()
        object OnGetAllGalley : FragEvent()
        data class DeleteMedia(val galleyMedia: GalleyMedia) : FragEvent()
        data class AddBucket(val name: String, val list: MutableList<GalleyMedia>) : FragEvent()
        data class OnGalleyUpdate(val updateList: MutableList<GalleyMedia>) : FragEvent()
        data class OnNewBucket(val pairs: Pair<Uri, Array<String>>) : FragEvent()
        data class OnListUpdate(val data: MutableList<GalleyMedia>?) : FragEvent()
        data class OnSelect(val galleyMedia: MutableList<GalleyMedia>) : FragEvent()
    }

    lateinit var rightGalley: String

    var isVideo: Boolean = false
    var isLock: Boolean = false

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

    fun sortData() = viewModelScope.launch(Dispatchers.IO) {
        galleyMap[R.string.all_galley.getString()] = mutableListOf()
        DatabaseHelper
            .instance
            .signedGalleyDAOBridge
            .run {
                val signedMedias = getAllMediaByType(isVideo) as MutableList<GalleyMedia>?
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
                if (!isLock) launch(Dispatchers.IO) {
                    signedMedias.forEach {
                        it.type?.forEach { type ->
                            if (galleyMap[type] == null) {
                                galleyMap[type] = mutableListOf()
                            }
                            galleyMap[type]?.add(it)
                        }
                    }
                }
                getAllGalley(isVideo)?.forEach {
                    galleyMap[it] = (getAllMediaByGalley(it, isVideo) as MutableList<GalleyMedia>)
                        .also { list ->
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

    fun updateGalley(
        updateList: MutableList<GalleyMedia>,
        callBack: (GalleyMedia.MediaStatus, GalleyMedia) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val allGalley = R.string.all_galley.getString()
        flow {
            updateList.forEach {
                when (it.mediaStatus) {
                    GalleyMedia.MediaStatus.Updated -> {
                        galleyMap[allGalley]?.first { media -> it.uri == media.uri }?.let { media ->
                            val allIndex = galleyMap[allGalley]?.indexOf(media)
                            if (allIndex != null && allIndex != -1) {
                                galleyMap[allGalley]?.set(allIndex, media)
                                val index = galleyMap[media.bucket]?.indexOf(media)
                                if (it.bucket != media.bucket) {
                                    galleyMap[media.bucket]?.remove(media)
                                    insertNewMedia(it)
                                    emit(Pair(GalleyMedia.MediaStatus.NewInsert, it))
                                    return@let
                                } else if (index != null && index != -1) {
                                    galleyMap[it.bucket]?.set(index, it)
                                }
                            }
                            emit(Pair(GalleyMedia.MediaStatus.Updated, media))
                        }
                    }
                    GalleyMedia.MediaStatus.Deleted -> {
                        galleyMap[it.bucket]?.remove(it)
                        galleyMap[allGalley]?.remove(it)
                        emit(Pair(GalleyMedia.MediaStatus.Deleted, it))
                    }
                    GalleyMedia.MediaStatus.NewInsert -> {
                        insertNewMedia(it)
                        galleyMap[allGalley]?.add(0, it)
                        emit(Pair(GalleyMedia.MediaStatus.NewInsert, it))
                    }
                }
            }
        }.buffer().collect {
            callBack.invoke(it.first, it.second)
        }
    }

    fun onTargetGalley(bucket: String): Boolean {
        return bucket == rightGalley || rightGalley == R.string.all_galley.getString()
    }

    private suspend fun insertNewMedia(it: GalleyMedia) {
        if (galleyMap[it.bucket] == null) {
            galleyMap[it.bucket] = mutableListOf()
            FragEvent.OnNewBucket(
                Pair(
                    it.uri,
                    arrayOf(it.bucket, galleyMap[it.bucket]?.size.toString())
                )
            ).apply { fragFlow.emit(this) }
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