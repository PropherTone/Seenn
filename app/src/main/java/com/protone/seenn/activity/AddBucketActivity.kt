package com.protone.seenn.activity

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.marginBottom
import com.protone.api.context.root
import com.protone.api.context.setSoftInputStatusListener
import com.protone.api.entity.MusicBucket
import com.protone.api.json.toUri
import com.protone.seenn.R
import com.protone.seenn.databinding.AddBucketActivityBinding
import com.protone.seenn.viewModel.AddBucketViewModel
import com.protone.seenn.viewModel.GalleyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddBucketActivity : BaseActivity<AddBucketActivityBinding, AddBucketViewModel>(false) {
    override val viewModel: AddBucketViewModel by viewModels()

    private var name: String
        set(value) {
            binding.musicBucketEnterName.setText(value)
        }
        get() = binding.musicBucketEnterName.text.toString()

    private var detail: String
        set(value) {
            binding.musicBucketEnterDetail.setText(value)
        }
        get() = binding.musicBucketEnterDetail.text.toString()

    private var uri: Uri? = null
        set(value) {
            Glide.with(this).load(value).into(binding.musicBucketIcon)
            field = value
        }

    override fun createView(): View {
        binding = AddBucketActivityBinding.inflate(layoutInflater, root, false)
        fitStatuesBarUsePadding(binding.root)
        binding.activity = this
        setSoftInputStatusListener { i, b ->
            if (b) {
                binding.root.marginBottom(i)
            } else {
                binding.root.marginBottom(0)
            }
        }
        return binding.root
    }

    override suspend fun AddBucketViewModel.init() {
        editName = intent.getStringExtra(AddBucketViewModel.BUCKET_NAME)
        editName?.let { eName ->
            musicBucket = getMusicBucketByName(eName)
            if (musicBucket == null) {
                R.string.come_up_unknown_error.toString().toast()
                finish()
                return@let
            } else {
                musicBucket?.let { refresh(it) }
            }
        }
    }

    fun chooseIcon() = launch(Dispatchers.Main) {
        startActivityForResult(
            GalleyActivity::class.intent.also { intent ->
                intent.putExtra(
                    GalleyViewModel.CHOOSE_MODE,
                    GalleyViewModel.CHOOSE_PHOTO
                )
            }
        ).let { result ->
            uri = result?.data?.getStringExtra(GalleyViewModel.URI)?.toUri()
        }
    }

    fun cancelAdd() {
        setResult(RESULT_CANCELED)
        finish()
    }

    fun confirm(): Unit = viewModel.run {
        launch {
            var intent: Intent?
            if (editName != null) {
                val re = musicBucket?.let {
                    updateMusicBucket(it, name, uri, detail)
                }
                if (re != 0 || re != -1) {
                    filterMusicBucket(name)
                    intent = Intent().putExtra(AddBucketViewModel.BUCKET_NAME, name)
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            } else addMusicBucket(name, uri, detail) { re, name ->
                if (re) {
                    intent = Intent().putExtra(AddBucketViewModel.BUCKET_NAME, name)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    private fun refresh(musicBucket: MusicBucket) {
        this.name = musicBucket.name
        detail = musicBucket.detail.toString()
        Glide.with(this).load(musicBucket.icon).into(binding.musicBucketIcon)
    }
}