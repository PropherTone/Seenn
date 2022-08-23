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
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toJson
import com.protone.ui.adapter.GalleyListAdapter
import com.protone.ui.itemDecoration.GalleyItemDecoration
import com.protone.seenn.R
import com.protone.seenn.databinding.GalleySearchActivityBinding
import com.protone.worker.viewModel.GalleySearchViewModel
import com.protone.worker.viewModel.GalleyViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleySearchActivity :
    BaseMediaActivity<GalleySearchActivityBinding, GalleySearchViewModel>(false),
    GalleyListAdapter.OnSelect, GalleySearchViewModel.OnQuery {
    override val viewModel: GalleySearchViewModel by viewModels()

    override fun createView(): View {
        binding = GalleySearchActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBar(binding.root)
        initPop()
        return binding.root
    }

    override suspend fun GalleySearchViewModel.init() {
        val searchModel = SearchModel(binding.inputSearch) {
            query(getInput())
        }
        onQueryListener = this@GalleySearchActivity
        val gainListData = getGainListData<GalleyMedia>()
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

    private fun newAdapter(list: MutableList<GalleyMedia>) = GalleyListAdapter(
        this@GalleySearchActivity,true
    ).also {
        it.setMedias(list)
        it.multiChoose = true
        it.setNewSelectList(viewModel.selectList)
        it.setOnSelectListener(this@GalleySearchActivity)
    }

    private fun initList() {
        binding.apply {
            linkInput(scroll, inputSearch)
            fun RecyclerView.initRecyclerView() {
                apply {
                    layoutManager = GridLayoutManager(context, 4)
                    adapter = newAdapter(mutableListOf())
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
                intent.getStringExtra("GALLEY") ?: R.string.all_galley.getString()
            )
        })
    }

    override fun popDelete() {
        tryDelete(viewModel.selectList) {
            binding.apply {
                if (it.size == 1) {
                    (resultGalleries.adapter as GalleyListAdapter).removeMedia(it[0])
                    (resultCato.adapter as GalleyListAdapter).removeMedia(it[0])
                    (resultNotes.adapter as GalleyListAdapter).removeMedia(it[0])
                } else if (it.size > 1) {
                    it.forEach { gm ->
                        (resultGalleries.adapter as GalleyListAdapter).removeMedia(gm)
                        (resultCato.adapter as GalleyListAdapter).removeMedia(gm)
                        (resultNotes.adapter as GalleyListAdapter).removeMedia(gm)
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
                putGainIntentData(selectList)
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
                binding.resultGalleries.swapAdapter(newAdapter(list), true)
            }
        }
    }

    override fun onCatoResult(list: MutableList<GalleyMedia>) {
        launch(Dispatchers.Main) {
            if (binding.resultCato.isGone) {
                listGone(binding.resultCato)
            }
            if (binding.resultCato.adapter is GalleyListAdapter) {
                binding.resultCato.swapAdapter(newAdapter(list), true)
            }
        }
    }

    override fun onNoteResult(list: MutableList<GalleyMedia>) {
        launch(Dispatchers.Main) {
            if (binding.resultNotes.isGone) {
                listGone(binding.resultNotes)
            }
            if (binding.resultNotes.adapter is GalleyListAdapter) {
                binding.resultNotes.swapAdapter(newAdapter(list), true)
            }
        }
    }

}