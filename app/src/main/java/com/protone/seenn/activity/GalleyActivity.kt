package com.protone.seenn.activity

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.context.root
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.seenn.databinding.GalleyActivityBinding
import com.protone.seenn.fragment.GalleyFragment
import com.protone.ui.R
import com.protone.ui.adapter.MyFragmentStateAdapter
import com.protone.worker.database.userConfig
import com.protone.worker.viewModel.GalleyFragmentViewModel
import com.protone.worker.viewModel.GalleyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleyActivity : BaseMediaActivity<GalleyActivityBinding, GalleyViewModel>(false) {
    override val viewModel: GalleyViewModel by viewModels()

    override fun createView(): View {
        binding = GalleyActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBar(binding.root)
        initPop()
        return binding.root
    }

    override suspend fun GalleyViewModel.init() {
        chooseType = intent.getStringExtra(GalleyViewModel.CHOOSE_MODE) ?: ""

        if (chooseType.isNotEmpty()) binding.galleyActionMenu.isVisible = false

        initPager(arrayListOf<Fragment>().apply {
            val action: suspend (value: GalleyFragmentViewModel.FragEvent) -> Unit = {
                when (it) {
                    is GalleyFragmentViewModel.FragEvent.OnSelect -> {
                        if (chooseData == null) chooseData = it.galleyMedia
                        if (it.galleyMedia.size > 0) onAction()
                    }
                    else -> {}
                }
            }
            add(
                GalleyFragment(
                    false,
                    userConfig.lockGalley.isNotEmpty(),
                    userConfig.combineGalley
                ) {
                    setMailer(frag1 = it.apply { launch { collect(action) } })
                })
            if (!userConfig.combineGalley) {
                add(GalleyFragment(true, userConfig.lockGalley.isNotEmpty(), false) {
                    setMailer(frag2 = it.apply { launch { collect(action) } })
                })
            }
        }, chooseType)
    }

    fun showPop() {
        showPop(binding.galleyActionMenu, (viewModel.chooseData?.size ?: 0) <= 0)
    }

    private suspend fun initPager(
        fragments: ArrayList<Fragment>,
        chooseType: String = "",
    ) = withContext(Dispatchers.Main) {
        if (chooseType.isNotEmpty()) {
            binding.galleyChooseConfirm.isGone = chooseType.isEmpty()
            binding.galleyChooseConfirm.setOnClickListener { confirm() }
        }
        binding.galleyPager.let {
            it.adapter = MyFragmentStateAdapter(
                this@GalleyActivity,
                if (userConfig.combineGalley) {
                    fragments
                } else when (chooseType) {
                    GalleyViewModel.CHOOSE_PHOTO -> arrayListOf(fragments[0])
                    GalleyViewModel.CHOOSE_VIDEO -> arrayListOf(fragments[1])
                    else -> fragments
                }
            )
            when (chooseType) {
                GalleyViewModel.CHOOSE_PHOTO -> arrayOf(R.string.photo)
                GalleyViewModel.CHOOSE_VIDEO -> arrayOf(R.string.video)
                else -> if (userConfig.combineGalley)
                    arrayOf(R.string.model_Galley) else arrayOf(R.string.photo, R.string.video)
            }.let { tabName ->
                TabLayoutMediator(binding.galleyTab.apply {
                    addOnTabSelectedListener(viewModel)
                }, it) { tab, position ->
                    tab.setText(
                        tabName[position]
                    )
                }.attach()
            }
        }
    }

    private fun confirm() {
        viewModel.chooseData?.let { list ->
            if (list.size > 0) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(GalleyViewModel.URI, list[0].uri.toUriJson())
                    putExtra(GalleyViewModel.GALLEY_DATA, list[0].toJson())
                })
            }
        }
        finish()
    }

    override fun popDelete() {
        viewModel.chooseData?.let {
            tryDelete(it) {}
        }
    }

    override fun popMoveTo() {
        viewModel.chooseData?.let {
            moveTo(binding.galleyBar, viewModel.rightMailer != 0, it) { target, list ->
                viewModel.addBucket(target, list)
            }
        }
    }

    override fun popRename() {
        viewModel.chooseData?.let {
            tryRename(it)
        }
    }

    override fun popSelectAll() {
        viewModel.selectAll()
    }

    override fun popSetCate() {
        viewModel.chooseData?.let { list ->
            addCate(list)
        }
    }

    override fun popIntoBox() {
        viewModel.intoBox()
    }

}