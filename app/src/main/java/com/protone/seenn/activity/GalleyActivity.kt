package com.protone.seenn.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.json.toJson
import com.protone.api.json.toUriJson
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.seen.R
import com.protone.seen.adapter.MyFragmentStateAdapter
import com.protone.seenn.databinding.GalleyActivityBinding
import com.protone.seenn.fragment.GalleyFragment
import com.protone.seenn.viewModel.GalleyViewModel
import com.protone.seenn.viewModel.GalleyViewViewModel
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleyActivity : BaseMediaActivity<GalleyActivityBinding, GalleyViewModel>(false) {
    override val viewModel: GalleyViewModel by viewModels()

    override fun initView() {
        binding = GalleyActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBar(binding.root)
        fitNavigationBar(binding.root)
        initPop()
    }


    override suspend fun onViewEvent(event: String) = Unit

    override suspend fun GalleyViewModel.init() = viewModel.run {
        chooseType = intent.getStringExtra(GalleyViewModel.CHOOSE_MODE) ?: ""

        if (chooseType.isNotEmpty()) showActionBtn()

        fun initPager() {
            initPager(arrayListOf<Fragment>().apply {
                add(GalleyFragment(false, userConfig.lockGalley.isNotEmpty()).also {
                    setMailer(frag1 = it.fragMailer)
                    it.iGalleyFragment = iGalleyFragment
                })
                add(GalleyFragment(true, userConfig.lockGalley.isNotEmpty()).also {
                    setMailer(frag2 = it.fragMailer)
                    it.iGalleyFragment = iGalleyFragment
                })
            }, chooseType)
        }

        startActivity = { galleyMedia: GalleyMedia, galley: String ->
            startActivity(GalleyViewActivity::class.intent.apply {
                putExtra(GalleyViewViewModel.MEDIA, galleyMedia.toJson())
                putExtra(GalleyViewViewModel.TYPE, galleyMedia.isVideo)
                putExtra(GalleyViewViewModel.GALLEY, galley)
            })
        }

        initPager()

        Medias.mediaLive.observe(this@GalleyActivity) {
            if (it == Medias.GALLEY_UPDATED) {
                initPager()
            }
        }

        chooseData.observe(this@GalleyActivity) {
            if (it.size > 0) {
                onAction()
            }
        }
    }

    fun showPop() {
        showPop(binding.galleyActionMenu, (viewModel.chooseData()?.size ?: 0) <= 0)
    }

    private fun showActionBtn() {
        binding.galleyActionMenu.isVisible = false
    }

    private fun initViewMode(chooseMode: Boolean) {
        if (chooseMode) {
            binding.galleyChooseConfirm.isGone = !chooseMode
            binding.galleyChooseConfirm.setOnClickListener { confirm() }
        }
    }

    private fun initPager(
        fragments: ArrayList<Fragment>,
        chooseType: String = "",
    ) = launch {
        initViewMode(chooseType.isNotEmpty())
        binding.galleyPager.let {
            it.adapter = MyFragmentStateAdapter(
                this@GalleyActivity,
                when (chooseType) {
                    GalleyViewModel.CHOOSE_PHOTO -> arrayListOf(fragments[0])
                    GalleyViewModel.CHOOSE_VIDEO -> arrayListOf(fragments[1])
                    else -> fragments
                }
            )
            TabLayoutMediator(binding.galleyTab.apply {
                addOnTabSelectedListener(viewModel)
            }, it) { tab, position ->
                tab.setText(
                    when (chooseType) {
                        GalleyViewModel.CHOOSE_PHOTO -> arrayOf(R.string.photo)
                        GalleyViewModel.CHOOSE_VIDEO -> arrayOf(R.string.video)
                        else -> arrayOf(R.string.photo, R.string.video)
                    }[position]
                )
            }.attach()
        }
    }

    private fun confirm() {
        viewModel.chooseData()?.let { list ->
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
        viewModel.chooseData()?.let {
            tryDelete(it) { re ->
                viewModel.deleteMedia(re)
            }
        }
    }

    override fun popMoveTo() {
        launch {
            viewModel.chooseData()?.let {
                moveTo(binding.galleyBar, viewModel.rightMailer != 0, it) { target, list ->
                    viewModel.addBucket(target, list)
                }
            }
        }
    }

    override fun popRename() {
        viewModel.chooseData()?.let {
            tryRename(it)
        }
    }

    override fun popSelectAll() {
        viewModel.selectAll()
    }

    override fun popSetCate() {
        viewModel.chooseData()?.let { list ->
            addCate(list)
        }
    }

    override fun popIntoBox() {
        launch(Dispatchers.IO) {
            viewModel.apply {
                IntentDataHolder.put((chooseData() ?: getChooseGalley()))
                startActivity(PictureBoxActivity::class.intent)
            }
        }
    }

}