package com.protone.seenn

import android.util.Log
import android.view.View
import androidx.activity.viewModels
import com.protone.api.TAG
import com.protone.api.context.root
import com.protone.seenn.activity.BaseActivity
import com.protone.seenn.databinding.ActivityTestBinding
import com.protone.seenn.viewModel.TestViewModel
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
        Medias.galleyNotifier1.collect {
            Log.d(TAG, "init: $it")
        }
    }

    fun clear() {
        binding.logView.text = ""
        viewModel.stringBuilder.clear()
    }

}