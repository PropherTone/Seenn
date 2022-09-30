package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.SearchModel
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.linkInput
import com.protone.api.context.root
import com.protone.api.entity.GalleryMedia
import com.protone.api.json.toJson
import com.protone.seenn.R
import com.protone.seenn.databinding.GallerySearchActivityBinding
import com.protone.ui.adapter.GalleryListAdapter
import com.protone.ui.itemDecoration.GalleryItemDecoration
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.GallerySearchViewModel
import com.protone.worker.viewModel.GalleryViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GallerySearchActivity : BaseMediaActivity<
        GallerySearchActivityBinding,
        GallerySearchViewModel,
        BaseViewModel.ViewEvent>(false),
    GalleryListAdapter.OnSelect, GallerySearchViewModel.OnQuery {
    override val viewModel: GallerySearchViewModel by viewModels()

    override fun createView(): GallerySearchActivityBinding {
        return GallerySearchActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@GallerySearchActivity
            fitStatuesBar(root)
            initPop()
        }
    }

    override suspend fun GallerySearchViewModel.init() {
        val searchModel = SearchModel(binding.inputSearch) {
            query(getInput())
        }
        onQueryListener = this@GallerySearchActivity
        val gainListData = getGainListData<GalleryMedia>()
        if (gainListData == null) {
            R.string.none.getString().toast()
        } else {
            data.addAll(gainListData)
            initList()
        }

        onFinish = {
            searchModel.destroy()
        }
    }

    private fun newAdapter(list: MutableList<GalleryMedia>) = GalleryListAdapter(
        this@GallerySearchActivity, true
    ).also {
        it.setMedias(list)
        it.multiChoose = true
        it.setNewSelectList(viewModel.selectList)
        it.setOnSelectListener(this@GallerySearchActivity)
    }

    private fun initList() {
        binding.apply {
            linkInput(scroll, inputSearch)
            fun RecyclerView.initRecyclerView() {
                apply {
                    layoutManager = GridLayoutManager(context, 4)
                    adapter = newAdapter(mutableListOf())
                    addItemDecoration(GalleryItemDecoration(paddingEnd))
                    linkInput(this, inputSearch)
                }
            }
            resultGalleries.initRecyclerView()
            resultCato.initRecyclerView()
            resultNotes.initRecyclerView()
            filterGallery.setOnClickListener {
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

    override fun select(galleryMedia: GalleryMedia) {
        binding.apply {
            (resultGalleries.adapter as GalleryListAdapter).noticeSelectChange(galleryMedia)
            (resultCato.adapter as GalleryListAdapter).noticeSelectChange(galleryMedia)
            (resultNotes.adapter as GalleryListAdapter).noticeSelectChange(galleryMedia)
        }
    }

    override fun select(galleryMedia: MutableList<GalleryMedia>) {
        if (galleryMedia.isEmpty()) {
            binding.apply {
                (resultGalleries.adapter as GalleryListAdapter).quitSelectMod()
                (resultCato.adapter as GalleryListAdapter).quitSelectMod()
                (resultNotes.adapter as GalleryListAdapter).quitSelectMod()
            }
        }
    }

    override fun openView(galleryMedia: GalleryMedia) {
        startActivity(GalleryViewActivity::class.intent.apply {
            putExtra(GalleryViewViewModel.MEDIA, galleryMedia.toJson())
            putExtra(GalleryViewViewModel.TYPE, galleryMedia.isVideo)
            putExtra(
                GalleryViewViewModel.GALLERY,
                intent.getStringExtra("gallery") ?: R.string.all_gallery.getString()
            )
        })
    }

    override fun popDelete() {
        tryDelete(viewModel.selectList) {
            binding.apply {
                if (it.size == 1) {
                    (resultGalleries.adapter as GalleryListAdapter).removeMedia(it[0])
                    (resultCato.adapter as GalleryListAdapter).removeMedia(it[0])
                    (resultNotes.adapter as GalleryListAdapter).removeMedia(it[0])
                } else if (it.size > 1) {
                    it.forEach { gm ->
                        (resultGalleries.adapter as GalleryListAdapter).removeMedia(gm)
                        (resultCato.adapter as GalleryListAdapter).removeMedia(gm)
                        (resultNotes.adapter as GalleryListAdapter).removeMedia(gm)
                    }
                }
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
        tryRename(viewModel.selectList)
    }

    override fun popSelectAll() {
        binding.apply {
            if (!resultGalleries.isGone) {
                (resultGalleries.adapter as GalleryListAdapter).selectAll()
            }
            if (!resultCato.isGone) {
                (resultCato.adapter as GalleryListAdapter).selectAll()
            }
            if (!resultNotes.isGone) {
                (resultNotes.adapter as GalleryListAdapter).selectAll()
            }
        }
    }

    override fun popSetCate() {
        addCate(viewModel.selectList)
    }

    override fun popIntoBox() {
        launch(Dispatchers.IO) {
            viewModel.apply {
                putGainIntentData(selectList)
                startActivity(PictureBoxActivity::class.intent)
            }
        }
    }

    override fun onGalleryResult(list: MutableList<GalleryMedia>) {
        if (list.isEmpty()) return
        launch(Dispatchers.Main) {
            if (binding.resultGalleries.isGone) {
                listGone(binding.resultGalleries)
            }
            if (binding.resultGalleries.adapter is GalleryListAdapter) {
                binding.resultGalleries.swapAdapter(newAdapter(list), false)
            }
        }
    }

    override fun onCatoResult(list: MutableList<GalleryMedia>) {
        if (list.isEmpty()) return
        launch(Dispatchers.Main) {
            if (binding.resultCato.isGone) {
                listGone(binding.resultCato)
            }
            if (binding.resultCato.adapter is GalleryListAdapter) {
                binding.resultCato.swapAdapter(newAdapter(list), false)
            }
        }
    }

    override fun onNoteResult(list: MutableList<GalleryMedia>) {
        if (list.isEmpty()) return
        launch(Dispatchers.Main) {
            if (binding.resultNotes.isGone) {
                listGone(binding.resultNotes)
            }
            if (binding.resultNotes.adapter is GalleryListAdapter) {
                binding.resultNotes.swapAdapter(newAdapter(list), false)
            }
        }
    }

}