package com.protone.seenn.activity

import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.baseType.launchIO
import com.protone.api.context.root
import com.protone.api.entity.GalleryMedia
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.seenn.databinding.GalleryActivityBinding
import com.protone.seenn.fragment.GalleryFragment
import com.protone.ui.R
import com.protone.ui.adapter.MyFragmentStateAdapter
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.GalleryViewModel
import com.protone.worker.viewModel.GalleryViewModel.Companion.CHOOSE_MEDIA
import com.protone.worker.viewModel.GalleryViewModel.Companion.CHOOSE_PHOTO
import com.protone.worker.viewModel.GalleryViewModel.Companion.CHOOSE_VIDEO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivity :
    BaseMediaActivity<GalleryActivityBinding, GalleryViewModel, BaseViewModel.ViewEvent>(false) {
    override val viewModel: GalleryViewModel by viewModels()

    override fun createView(): GalleryActivityBinding {
        return GalleryActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@GalleryActivity
            fitStatuesBar(root)
            initPop()
        }
    }

    override suspend fun GalleryViewModel.init() {
        val chooseType = intent.getStringExtra(GalleryViewModel.CHOOSE_MODE) ?: ""

        if (chooseType.isNotEmpty()) {
            binding.galleryActionMenu.isVisible = false
            binding.galleryChooseConfirm.isGone = chooseType.isEmpty()
            binding.galleryChooseConfirm.setOnClickListener { confirm() }
        }
        initPager(chooseType)
    }

    private suspend fun GalleryViewModel.initPager(
        chooseType: String = "",
    ) = withContext(Dispatchers.Main) {
        val combine = userConfig.combineGallery || chooseType == CHOOSE_MEDIA
        binding.galleryPager.adapter = MyFragmentStateAdapter(
            this@GalleryActivity,
            mutableListOf<Fragment>().also { fs ->
                val lock = userConfig.lockGallery.isNotEmpty()
                when (chooseType) {
                    CHOOSE_PHOTO ->
                        fs.add(GalleryFragment(false, lock, false) { f -> setMailer(frag1 = f) })
                    CHOOSE_VIDEO ->
                        fs.add(GalleryFragment(true, lock, false) { f -> setMailer(frag2 = f) })
                    else -> {
                        fs.add(GalleryFragment(false, lock, combine) { f -> setMailer(frag1 = f) })
                        if (!combine) fs.add(GalleryFragment(
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
                if (combine) arrayOf(R.string.model_gallery)
                else arrayOf(R.string.photo, R.string.video)
            }
        }.let { tabList ->
            TabLayoutMediator(
                binding.galleryTab.apply { addOnTabSelectedListener(viewModel) },
                binding.galleryPager
            ) { tab, position -> tab.setText(tabList[position]) }.attach()
        }
    }

    private fun confirm() {
        viewModel.chooseData?.let { list ->
            if (list.size <= 0) return
            setResult(
                RESULT_OK,
                Intent().putExtra(GalleryViewModel.URI, list[0].uri.toUriJson())
                    .putExtra(GalleryViewModel.Gallery_DATA, list[0].toJson())
            )
        }
        finish()
    }

    fun showPop() {
        showPop(binding.galleryActionMenu, (viewModel.chooseData?.size ?: 0) <= 0)
    }

    override fun popDelete() {
        viewModel.chooseData?.let {
            tryDelete(it) {}
        }
    }

    override fun popMoveTo() {
        viewModel.chooseData?.let {
            if (it.size <= 0) return
            moveTo(binding.galleryBar, it[0].isVideo, it) { target, list ->
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