package com.protone.seen

import android.content.Context
import android.transition.TransitionManager
import android.view.View
import androidx.core.view.isGone
import androidx.recyclerview.widget.GridLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.itemDecoration.GalleyItemDecoration
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seen.databinding.GalleySearchLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleySearchSeen(context: Context) : Seen<GalleySearchSeen.SearchEvent>(context) {

    enum class SearchEvent {
        Finish,
        Query
    }

    private val binding =
        GalleySearchLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View = binding.root
    override fun getToolBar(): View = binding.bar

    val queryText = binding.inputSearch

    init {
        setSettleToolBar()
        binding.self = this
    }

    override fun offer(event: SearchEvent) {
        viewEvent.offer(event)
    }

    fun initList(
        isVideo: Boolean,
        listener: GalleyListAdapter.OnSelect
    ) {
        binding.apply {
            resultGalleries.apply {
                layoutManager = GridLayoutManager(context, 4)
                adapter = GalleyListAdapter(context, mutableListOf(), isVideo, false).also {
                    it.multiChoose = true
                    it.setOnSelectListener(listener)
                }
                addItemDecoration(GalleyItemDecoration(paddingEnd))
            }
            resultCato.apply {
                layoutManager = GridLayoutManager(context, 4)
                adapter = GalleyListAdapter(context, mutableListOf(), isVideo, false).also {
                    it.multiChoose = true
                    it.setOnSelectListener(listener)
                }
                addItemDecoration(GalleyItemDecoration(paddingEnd))
            }
            resultNotes.apply {
                layoutManager = GridLayoutManager(context, 4)
                adapter = GalleyListAdapter(context, mutableListOf(), isVideo, false).also {
                    it.multiChoose = true
                    it.setOnSelectListener(listener)
                }
                addItemDecoration(GalleyItemDecoration(paddingEnd))
            }
            filterGalley.setOnClickListener {
                listGone(binding.resultGalleries)
            }
            filterCato.setOnClickListener {
                listGone(binding.resultCato)
            }
            filterNote.setOnClickListener {
                listGone(binding.resultNotes)
            }
        }
    }

    suspend fun refreshGalleyList(item: MutableList<GalleyMedia>?) = withContext(Dispatchers.Main) {
        if (binding.resultGalleries.isGone) {
            listGone(binding.resultGalleries)
        }
        if (binding.resultGalleries.adapter is GalleyListAdapter) {
            (binding.resultGalleries.adapter as GalleyListAdapter).noticeDataUpdate(item)
        }
    }

    suspend fun refreshCatoList(item: MutableList<GalleyMedia>?) = withContext(Dispatchers.Main) {
        if (binding.resultCato.isGone) {
            listGone(binding.resultCato)
        }
        if (binding.resultCato.adapter is GalleyListAdapter) {
            (binding.resultCato.adapter as GalleyListAdapter).noticeDataUpdate(item)
        }
    }

    suspend fun refreshNoteList(item: MutableList<GalleyMedia>?) = withContext(Dispatchers.Main) {
        if (binding.resultNotes.isGone) {
            listGone(binding.resultNotes)
        }
        if (binding.resultNotes.adapter is GalleyListAdapter) {
            (binding.resultNotes.adapter as GalleyListAdapter).noticeDataUpdate(item)
        }
    }

    private fun listGone(view: View) {
        TransitionManager.beginDelayedTransition(binding.resultContainer)
        view.isGone = !view.isGone
    }
}