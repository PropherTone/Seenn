package com.protone.seenn

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toUri
import com.protone.api.toMediaBitmapByteArray
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Medias
import com.protone.seen.AddBucketSeen
import com.protone.seen.GalleySeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class AddBucketActivity : BaseActivity<AddBucketSeen>() {

    companion object {
        @JvmStatic
        val BUCKET_NAME = "BUCKET_NAME"
    }

    override suspend fun main() {
        val addBucketSeen = AddBucketSeen(this)
        setContentSeen(addBucketSeen)

        val name = intent.getStringExtra(BUCKET_NAME)
        var musicBucket: MusicBucket? = null
        if (name != null) {
            addBucketSeen.refresh(name)
            musicBucket =
                withContext(Dispatchers.IO) { DataBaseDAOHelper.getMusicBucketByName(name) }
            if (musicBucket == null) {
                toast(getString(R.string.come_up_unknown_error))
                finish()
                return
            }
        }

        while (isActive) {
            select<Unit> {
                event.onReceive {

                }
                addBucketSeen.viewEvent.onReceive {
                    when (it) {
                        AddBucketSeen.Event.ChooseIcon -> {
                            startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                GalleyActivity::class.intent.also { intent ->
                                    intent.putExtra(
                                        GalleyActivity.CHOOSE_MODE,
                                        GalleySeen.CHOOSE_PHOTO
                                    )
                                }
                            )?.let { result ->
                                addBucketSeen.uri =
                                    result.data?.getStringExtra(GalleyActivity.URI)?.toUri()
                            }
                        }
                        AddBucketSeen.Event.Confirm ->
                            if (name != null) {
                                val re = addBucketSeen.updateMusicBucket(musicBucket!!)
                                Intent().apply {
                                    setResult(
                                        if (re != 0 || re != -1) {
                                            if (Medias.musicBucket.containsKey(name)) {
                                                Medias.musicBucket[addBucketSeen.name] =
                                                    Medias.musicBucket.remove(name)
                                                        ?: mutableListOf()
                                            }
                                            putExtra(BUCKET_NAME, addBucketSeen.name)
                                            RESULT_OK
                                        } else RESULT_CANCELED, this
                                    )
                                }
                                finish()
                            } else DataBaseDAOHelper.addMusicBucketWithCallBack(
                                MusicBucket(
                                    addBucketSeen.name,
                                    addBucketSeen.uri?.toMediaBitmapByteArray(),
                                    0,
                                    addBucketSeen.detail,
                                    todayDate("yyyy/MM/dd")
                                )
                            ) { re, name ->
                                Intent().apply {
                                    setResult(
                                        if (re) {
                                            putExtra(BUCKET_NAME, name)
                                            RESULT_OK
                                        } else RESULT_CANCELED, this
                                    )
                                }
                                finish()
                            }
                        AddBucketSeen.Event.Finished -> {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private suspend fun AddBucketSeen.refresh(name: String) = withContext(Dispatchers.IO) {
        val musicBucket = DataBaseDAOHelper.getMusicBucketByName(name)
        withContext(Dispatchers.Main) {
            this@refresh.name = musicBucket?.name.toString()
            this@refresh.detail = musicBucket?.detail.toString()
            this@refresh.loadIcon(musicBucket?.icon)
        }
    }

    private suspend fun AddBucketSeen.updateMusicBucket(
        musicBucket: MusicBucket
    ) = DataBaseDAOHelper.updateMusicBucketRs(
        musicBucket.also { mb ->
            if (mb.name != name) mb.name = name
            val toMediaBitmapByteArray = uri?.toMediaBitmapByteArray()
            if (!mb.icon.contentEquals(toMediaBitmapByteArray)) mb.icon = toMediaBitmapByteArray
            if (mb.detail != detail) mb.detail = detail
            todayDate("yyyy/MM/dd")
        }
    )

}