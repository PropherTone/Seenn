package com.protone.seenn.activity

import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.root
import com.protone.api.entity.GalleyMedia
import com.protone.seen.adapter.PictureBoxAdapter
import com.protone.seenn.R
import com.protone.seenn.databinding.PictureBoxActivityBinding
import com.protone.seenn.viewModel.PictureBoxViewModel

class PictureBoxActivity:BaseActivity<PictureBoxActivityBinding, PictureBoxViewModel>(false) {
    override val viewModel: PictureBoxViewModel by viewModels()

    override fun createView(): View {
        binding = PictureBoxActivityBinding.inflate(layoutInflater, root, false)
        return binding.root
    }

    override suspend fun PictureBoxViewModel.init() {
        val gainListData = getGainListData<GalleyMedia>()
        if (gainListData != null) {
            initPictureBox(gainListData as MutableList<GalleyMedia>)
        }else{
            R.string.failed_msg.getString().toast()
            finish()
        }
    }

    private fun initPictureBox(picUri: MutableList<GalleyMedia>) {
        binding.picView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = PictureBoxAdapter(context, picUri)
        }
    }
}