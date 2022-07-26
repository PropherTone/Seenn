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
import kotlinx.coroutines.launch
import kotlin.streams.toList

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
        callBack: (Boolean, Pair<Uri, Array<String>>) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        var isNew: Boolean
        DatabaseHelper
            .instance
            .signedGalleyDAOBridge
            .getAllMediaByType(isVideo)
            ?.let { list ->
                galleyMap[R.string.all_galley.getString()] = list as MutableList<GalleyMedia>
            }
        updateList.stream().map { galley -> galley.bucket }.toList().forEach { bucket ->
            isNew = galleyMap[bucket] == null

            galleyMap[bucket] = DatabaseHelper.instance.signedGalleyDAOBridge
                .getAllMediaByGalley(bucket, isVideo) as MutableList<GalleyMedia>

            galleyMap[bucket]?.let { list ->
                val pair = Pair(
                    if (list.size > 0) list[0].uri else Uri.EMPTY,
                    arrayOf(bucket, list.size.toString())
                )
                callBack.invoke(isNew, pair)
            }
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

}