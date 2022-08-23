package com.protone.seenn

import android.util.Log
import android.view.View
import androidx.activity.viewModels
import com.protone.api.TAG
import com.protone.api.context.root
import com.protone.worker.activity.BaseActivity
import com.protone.worker.databinding.ActivityTestBinding
import com.protone.worker.viewModel.TestViewModel
import kotlinx.coroutines.flow.collect

class TestActivity : BaseActivity<ActivityTestBinding, TestViewModel>(false) {
    override val viewModel: TestViewModel by viewModels()

    override fun createView(): View {
        binding = ActivityTestBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBar(binding.root)
        return binding.root
    }

    override suspend fun TestViewModel.init() {
        viewModel.setLogView(binding.logView)
        binding.model = viewModel
        binding.sImageView.setImageResource("long.jpeg")
        binding.sImageView2.setImageResource("long.jpeg")
        binding.sImageView3.setImageResource("long.jpeg")
        binding.sImageView4.setImageResource("long.jpeg")
        binding.sImageView5.setImageResource("long.jpeg")
        binding.sImageView6.setImageResource("long.jpeg")
        Medias.galleyNotifier.collect {
            Log.d(TAG, "init: $it")
        }
    }

    fun clear() {
        binding.logView.text = ""
        viewModel.stringBuilder.clear()
    }

}