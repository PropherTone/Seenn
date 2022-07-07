package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.SearchModel
import com.protone.api.context.intent
import com.protone.api.context.linkInput
import com.protone.api.context.root
import com.protone.api.json.toJson
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seen.itemDecoration.GalleyItemDecoration
import com.protone.seenn.R
import com.protone.seenn.databinding.GalleySearchActivityBinding
import com.protone.seenn.viewModel.GalleySearchViewModel
import com.protone.seenn.viewModel.GalleyViewViewModel
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleySearchActivity :
    BaseMediaActivity<GalleySearchActivityBinding, GalleySearchViewModel>(false),
    GalleyListAdapter.OnSelect, GalleySearchViewModel.OnQuery {
    override val viewModel: GalleySearchViewModel by viewModels()

    override fun initView() {
        binding = GalleySearchActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitNavigationBar(binding.root)
        fitStatuesBar(binding.root)
        initPop()
    }

    override suspend fun GalleySearchViewModel.init() {
        SearchModel(binding.inputSearch) {
            query(getInput())
        }
        onQueryListener = this@GalleySearchActivity
        IntentDataHolder.get().let {
            if (!(it != null && it is List<*> && it.isNotEmpty() && it[0] is GalleyMedia)) {
                toast(getString(R.string.none))
            } else {
                @Suppress("UNCHECKED_CAST")
                data.addAll(it as List<GalleyMedia>)
                initList(data[0].isVideo)
            }
        }
    }

    override suspend fun onViewEvent(event: String) = Unit

    private fun initList(isVideo: Boolean) {
        binding.apply {
            linkInput(scroll, inputSearch)
            fun RecyclerView.initRecyclerView() {
                apply {
                    layoutManager = GridLayoutManager(context, 4)
                    adapter = GalleyListAdapter(
                        this@GalleySearchActivity,
                        mutableListOf(), isVideo, true
                    ).also {
                        it.multiChoose = true
                        it.setNewSelectList(viewModel.selectList)
                        it.setOnSelectListener(this@GalleySearchActivity)
                    }
                    addItemDecoration(GalleyItemDecoration(paddingEnd))
                    linkInput(this, inputSearch)
                }
            }
            resultGalleries.initRecyclerView()
            resultCato.initRecyclerView()
            resultNotes.initRecyclerView()
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

    private fun listGone(view: View) {
        TransitionManager.beginDelayedTransition(binding.resultContainer)
        view.isGone = !view.isGone
    }

    fun showPop() {
        showPop(binding.actionMenu, false)
    }

    override fun select(galleyMedia: GalleyMedia) {
        binding.apply {
            (resultGalleries.adapter as GalleyListAdapter).noticeSelectChange(galleyMedia)
            (resultCato.adapter as GalleyListAdapter).noticeSelectChange(galleyMedia)
            (resultNotes.adapter as GalleyListAdapter).noticeSelectChange(galleyMedia)
        }
    }

    override fun select(galleyMedia: MutableList<GalleyMedia>) {
        if (galleyMedia.isEmpty()) {
            binding.apply {
                (resultGalleries.adapter as GalleyListAdapter).quitSelectMod()
                (resultCato.adapter as GalleyListAdapter).quitSelectMod()
                (resultNotes.adapter as GalleyListAdapter).quitSelectMod()
            }
        }
    }

    override fun openView(galleyMedia: GalleyMedia) {
        startActivity(GalleyViewActivity::class.intent.apply {
            putExtra(GalleyViewViewModel.MEDIA, galleyMedia.toJson())
            putExtra(GalleyViewViewModel.TYPE, galleyMedia.isVideo)
            putExtra(
                GalleyViewViewModel.GALLEY,
                intent.getStringExtra("GALLEY") ?: getString(R.string.all_galley)
            )
        })
    }

    override fun popDelete() {
        tryDelete(viewModel.selectList, this) {
            binding.apply {
                (resultGalleries.adapter as GalleyListAdapter).removeMedia(it)
                (resultCato.adapter as GalleyListAdapter).removeMedia(it)
                (resultNotes.adapter as GalleyListAdapter).removeMedia(it)
            }
        }
    }

    override fun popMoveTo() {
        launch {
            viewModel.selectList.let {
                moveTo(binding.toolbar, it[0].isVideo, it) { _, _ -> }
            }
        }
    }

    override fun popRename() {
        tryRename(viewModel.selectList, this)
    }

    override fun popSelectAll() {
        binding.apply {
            if (!resultGalleries.isGone) {
                (resultGalleries.adapter as GalleyListAdapter).selectAll()
            }
            if (!resultCato.isGone) {
                (resultCato.adapter as GalleyListAdapter).selectAll()
            }
            if (!resultNotes.isGone) {
                (resultNotes.adapter as GalleyListAdapter).selectAll()
            }
        }
    }

    override fun popSetCate() {
        addCate(viewModel.selectList)
    }

    override fun popIntoBox() {
        launch(Dispatchers.IO) {
            viewModel.apply {
                IntentDataHolder.put(selectList)
                startActivity(PictureBoxActivity::class.intent)
            }
        }
    }

    override fun onGalleyResult(list: MutableList<GalleyMedia>) {
        launch(Dispatchers.Main) {
            if (binding.resultGalleries.isGone) {
                listGone(binding.resultGalleries)
            }
            if (binding.resultGalleries.adapter is GalleyListAdapter) {
                (binding.resultGalleries.adapter as GalleyListAdapter).noticeDataUpdate(list)
            }
        }
    }

    override fun onCatoResult(list: MutableList<GalleyMedia>) {
        launch(Dispatchers.Main) {
            if (binding.resultCato.isGone) {
                listGone(binding.resultCato)
            }
            if (binding.resultCato.adapter is GalleyListAdapter) {
                (binding.resultCato.adapter as GalleyListAdapter).noticeDataUpdate(list)
            }
        }
    }

    override fun onNoteResult(list: MutableList<GalleyMedia>) {
        launch(Dispatchers.Main) {
            if (binding.resultNotes.isGone) {
                listGone(binding.resultNotes)
            }
            if (binding.resultNotes.adapter is GalleyListAdapter) {
                (binding.resultNotes.adapter as GalleyListAdapter).noticeDataUpdate(list)
            }
        }
    }

}