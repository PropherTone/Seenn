package com.protone.seenn.activity

import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.root
import com.protone.api.entity.GalleryMedia
import com.protone.seenn.R
import com.protone.seenn.databinding.PictureBoxActivityBinding
import com.protone.ui.adapter.PictureBoxAdapter
import com.protone.ui.customView.ScalableRegionLoadingImageView
import com.protone.ui.customView.bitmapCache
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.PictureBoxViewModel

class PictureBoxActivity :
    BaseActivity<PictureBoxActivityBinding, PictureBoxViewModel, BaseViewModel.ViewEvent>(false) {
    override val viewModel: PictureBoxViewModel by viewModels()

    override fun createView(): PictureBoxActivityBinding {
        return PictureBoxActivityBinding.inflate(layoutInflater, root, false)
    }

    override suspend fun PictureBoxViewModel.init() {
        val gainListData = getGainListData<GalleryMedia>()
        if (gainListData != null) {
            initPictureBox(gainListData as MutableList<GalleryMedia>)
        } else {
            R.string.no_data.getString().toast()
            finish()
        }
    }

    private fun initPictureBox(picUri: MutableList<GalleryMedia>) {
        binding.picView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = PictureBoxAdapter(context, picUri)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                    (layoutManager as LinearLayoutManager).also {
                        val firstVisible = it.findFirstVisibleItemPosition()
                        val lastVisible = it.findLastVisibleItemPosition()
                        for (i in firstVisible..lastVisible) {
                            when (val child = it.findViewByPosition(i)) {
                                is ScalableRegionLoadingImageView -> if (child.isLongImage()) {
                                    child.reZone()
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    override suspend fun doFinish() {
        bitmapCache.evictAll()
    }
}