package com.protone.seenn.activity

import android.content.Intent
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
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.GalleyViewModel
import com.protone.worker.viewModel.GalleyViewModel.Companion.CHOOSE_PHOTO
import com.protone.worker.viewModel.GalleyViewModel.Companion.CHOOSE_VIDEO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleyActivity :
    BaseMediaActivity<GalleyActivityBinding, GalleyViewModel, BaseViewModel.ViewEvent>(false) {
    override val viewModel: GalleyViewModel by viewModels()

    override fun createView(): GalleyActivityBinding {
        return GalleyActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@GalleyActivity
            fitStatuesBar(binding.root)
            initPop()
        }
    }

    override suspend fun GalleyViewModel.init() {
        val chooseType = intent.getStringExtra(GalleyViewModel.CHOOSE_MODE) ?: ""

        if (chooseType.isNotEmpty()) {
            binding.galleyActionMenu.isVisible = false
            binding.galleyChooseConfirm.isGone = chooseType.isEmpty()
            binding.galleyChooseConfirm.setOnClickListener { confirm() }
        }
        initPager(chooseType)
    }

    private suspend fun GalleyViewModel.initPager(
        chooseType: String = "",
    ) = withContext(Dispatchers.Main) {
        val combine = userConfig.combineGalley
        binding.galleyPager.adapter = MyFragmentStateAdapter(
            this@GalleyActivity,
            mutableListOf<Fragment>().also { fs ->
                val lock = userConfig.lockGalley.isNotEmpty()
                when (chooseType) {
                    CHOOSE_PHOTO ->
                        fs.add(GalleyFragment(false, lock, false) { f -> setMailer(frag1 = f) })
                    CHOOSE_VIDEO ->
                        fs.add(GalleyFragment(true, lock, false) { f -> setMailer(frag2 = f) })
                    else -> {
                        fs.add(GalleyFragment(false, lock, combine) { f -> setMailer(frag1 = f) })
                        if (!combine) fs.add(GalleyFragment(
                            true,
                            lock,
                            false
                        ) { f -> setMailer(frag2 = f) })
                    }
                }
            }
        )
        when (chooseType) {
            CHOOSE_PHOTO -> arrayOf(R.string.photo)
            CHOOSE_VIDEO -> arrayOf(R.string.video)
            else -> {
                if (combine) arrayOf(R.string.model_Galley)
                else arrayOf(R.string.photo, R.string.video)
            }
        }.let { tabList ->
            TabLayoutMediator(
                binding.galleyTab.apply { addOnTabSelectedListener(viewModel) },
                binding.galleyPager
            ) { tab, position -> tab.setText(tabList[position]) }.attach()
        }
    }

    private fun confirm() {
        viewModel.chooseData?.let { list ->
            if (list.size <= 0) return
            setResult(
                RESULT_OK,
                Intent().putExtra(GalleyViewModel.URI, list[0].uri.toUriJson())
                    .putExtra(GalleyViewModel.GALLEY_DATA, list[0].toJson())
            )
        }
        finish()
    }

    fun showPop() {
        showPop(binding.galleyActionMenu, (viewModel.chooseData?.size ?: 0) <= 0)
    }

    override fun popDelete() {
        viewModel.chooseData?.let {
            tryDelete(it) {}
        }
    }

    override fun popMoveTo() {
        viewModel.chooseData?.let {
            if (it.size <= 0) return
            moveTo(binding.galleyBar, it[0].isVideo, it) { target, list ->
                viewModel.addBucket(target, list)
            }
        }
    }

    override fun popRename() {
        viewModel.chooseData?.let {
            if (it.size <= 0) return
            tryRename(it)
        }
    }

    override fun popSelectAll() {
        viewModel.selectAll()
    }

    override fun popSetCate() {
        viewModel.chooseData?.let { list ->
            if (list.size <= 0) return
            addCate(list)
        }
    }

    override fun popIntoBox() {
        viewModel.intoBox()
    }

}