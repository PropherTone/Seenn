package com.protone.seenn.activity

import android.content.Intent
import android.net.Uri
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
import com.protone.worker.viewModel.AddBucketViewModel
import com.protone.worker.viewModel.GalleyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddBucketActivity : BaseActivity<
        AddBucketActivityBinding,
        AddBucketViewModel,
        AddBucketViewModel.AddBucketEvent>(true) {
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

    override fun createView(): AddBucketActivityBinding {
        return AddBucketActivityBinding.inflate(layoutInflater, root, false).apply {
            fitStatuesBarUsePadding(root)
            activity = this@AddBucketActivity
            setSoftInputStatusListener { i, b ->
                if (b) {
                    root.marginBottom(i)
                } else {
                    root.marginBottom(0)
                }
            }
        }
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

        onViewEvent {
            when (it) {
                AddBucketViewModel.AddBucketEvent.ChooseIcon -> chooseIcon()
                AddBucketViewModel.AddBucketEvent.Confirm -> confirm()
                AddBucketViewModel.AddBucketEvent.Cancel -> cancelAdd()
            }
        }
    }

    private suspend fun chooseIcon() = withContext(Dispatchers.Main) {
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

    private suspend fun cancelAdd() = withContext(Dispatchers.Default) {
        setResult(RESULT_CANCELED)
        finish()
    }

    private suspend fun AddBucketViewModel.confirm() = withContext(Dispatchers.Main) {
        var intent: Intent?
        if (editName != null) {
            val re = musicBucket?.let {
                updateMusicBucket(it, name, uri, detail)
            }
            if (re != 0 || re != -1) {
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

    private suspend fun refresh(musicBucket: MusicBucket) = withContext(Dispatchers.Main) {
        this@AddBucketActivity.name = musicBucket.name
        detail = musicBucket.detail.toString()
        Glide.with(this@AddBucketActivity).load(musicBucket.icon).into(binding.musicBucketIcon)
    }
}