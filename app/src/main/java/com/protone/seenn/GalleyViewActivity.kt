package com.protone.seenn

import com.bumptech.glide.Glide
import com.protone.api.context.intent
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.getStorageSize
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.json.toUri
import com.protone.api.toDateString
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.GalleyViewSeen
import com.protone.seen.databinding.ImageCateLayoutBinding
import com.protone.seen.databinding.TextCateLayoutBinding
import com.protone.seenn.viewModel.GalleyViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

class GalleyViewActivity : BaseActivity<GalleyViewSeen>() {

    private var curPosition: Int = 0
    private lateinit var galleyMedias: MutableList<GalleyMedia>

    override suspend fun main() {
        val galleyViewSeen = GalleyViewSeen(this)
        setContentSeen(galleyViewSeen)

        val isVideo = intent.getBooleanExtra(GalleyViewViewModel.TYPE, false)

        galleyMedias = withContext(Dispatchers.IO) {
            val galley = intent.getStringExtra(GalleyViewViewModel.GALLEY) ?: getString(R.string.all_galley)
            var allMedia = (DataBaseDAOHelper.getAllMediaByType(isVideo)
                ?: mutableListOf()) as MutableList<GalleyMedia>
            if (galley != this@GalleyViewActivity.getString(R.string.all_galley)) allMedia =
                allMedia.stream().filter {
                    (it.bucket == galley) || (it.type?.contains(galley) == true)
                }.collect(Collectors.toList())
            allMedia
        }
        galleyViewSeen.init(isVideo)

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                galleyViewSeen.viewEvent.onReceive {
                    when (it) {
                        GalleyViewSeen.GalleyVEvent.Finish -> finish()
                        GalleyViewSeen.GalleyVEvent.ShowAction -> galleyViewSeen.showPop()
                        GalleyViewSeen.GalleyVEvent.SetNotes -> galleyViewSeen.setInfo()
                        GalleyViewSeen.GalleyVEvent.AddCato -> {}
                        GalleyViewSeen.GalleyVEvent.Delete -> {}
                        GalleyViewSeen.GalleyVEvent.IntoBox -> {}
                        GalleyViewSeen.GalleyVEvent.MoveTo -> {}
                        GalleyViewSeen.GalleyVEvent.Rename -> {}
                        GalleyViewSeen.GalleyVEvent.SelectAll -> {}
                    }
                }
            }
        }
    }

    private suspend fun GalleyViewSeen.init(isVideo: Boolean) {
        initList {
            startActivity(NoteViewActivity::class.intent.apply {
                putExtra(NoteViewActivity.NOTE_NAME, it)
            })
        }
        val mediaIndex = getMediaIndex()
        initViewPager(mediaIndex, galleyMedias, isVideo) { position ->
            curPosition = position
            setMediaInfo(position)
        }
        setMediaInfo(mediaIndex)
        setInfo()
    }

    private suspend fun GalleyViewSeen.setInfo() {
        val galleyMedia = DataBaseDAOHelper.getSignedMediaRs(galleyMedias[curPosition].uri)
        removeCato()
        galleyMedia?.cate?.onEach {
            if (it.contains("content://")) {
                addCato(
                    ImageCateLayoutBinding.inflate(
                        context.layoutInflater,
                        context.root,
                        false
                    ).apply {
                        Glide.with(context).asDrawable().load(it.toUri()).into(catoBack)
                        catoName.text = galleyMedia.name
                        root.setOnClickListener {
                            startActivity(GalleyViewActivity::class.intent.apply {
                                putExtra(GalleyViewViewModel.MEDIA, galleyMedia.toJson())
                                putExtra(GalleyViewViewModel.TYPE, galleyMedia.isVideo)
                            })
                        }
                    }.root
                )
            } else {
                addCato(
                    TextCateLayoutBinding.inflate(
                        context.layoutInflater,
                        context.root,
                        false
                    ).apply {
                        cato.text = it
                    }.root
                )
            }
        }
        setNotes(((galleyMedia?.notes ?: mutableListOf()) as MutableList<String>))
    }

    private fun GalleyViewSeen.setMediaInfo(position: Int) = galleyMedias[position].let { m ->
        setMediaInfo(
            m.name,
            m.date.toDateString("yyyy/MM/dd").toString(),
            m.size.getStorageSize(),
            m.path ?: m.uri.toString()
        )
    }

    private suspend fun getMediaIndex() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Int> { co ->
            val galleyMedia =
                intent.getStringExtra(GalleyViewViewModel.MEDIA)?.toEntity(GalleyMedia::class.java)
            val indexOf = galleyMedias.indexOf(galleyMedia)
            curPosition = indexOf
            co.resumeWith(Result.success(indexOf))
        }
    }

}