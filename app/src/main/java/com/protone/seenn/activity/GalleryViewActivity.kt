package com.protone.seenn.activity

import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.protone.api.baseType.getStorageSize
import com.protone.api.baseType.getString
import com.protone.api.baseType.toDateString
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.GalleryMedia
import com.protone.api.json.toEntity
import com.protone.api.onResult
import com.protone.seenn.R
import com.protone.seenn.databinding.GalleryViewActivityBinding
import com.protone.seenn.fragment.GalleryViewFragment
import com.protone.ui.adapter.CatoListAdapter
import com.protone.ui.adapter.CheckListAdapter
import com.protone.worker.viewModel.GalleryViewViewModel
import com.protone.worker.viewModel.NoteViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewActivity : BaseMediaActivity<
        GalleryViewActivityBinding,
        GalleryViewViewModel,
        GalleryViewViewModel.GalleryViewEvent>(true) {
    override val viewModel: GalleryViewViewModel by viewModels()

    override fun createView(): GalleryViewActivityBinding {
        return GalleryViewActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@GalleryViewActivity
            fitStatuesBarUsePadding(galleryVCover)
            initPop()
            popLayout?.galleryIntoBox?.isGone = true
            popLayout?.gallerySelectAll?.isGone = true
            next.setOnClickListener {
                galleryVView.setCurrentItem(1 + galleryVView.currentItem, true)
            }
            previous.setOnClickListener {
                galleryVView.setCurrentItem(galleryVView.currentItem - 1, true)
            }
            galleryVLinks.apply {
                adapter = CheckListAdapter(this@GalleryViewActivity, check = false).also {
                    it.startNote = {
                        startActivity(NoteViewActivity::class.intent.apply {
                            putExtra(NoteViewViewModel.NOTE_NAME, it)
                        })
                    }
                }
                layoutManager = LinearLayoutManager(context)
            }
            galleryVCatoContainer.apply {
                adapter = CatoListAdapter(this@GalleryViewActivity,
                    object : CatoListAdapter.CatoListDataProxy {
                        override fun getMedia(): GalleryMedia {
                            return viewModel.getCurrentMedia()
                        }
                    })
                layoutManager = LinearLayoutManager(context).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
        }
    }

    override suspend fun GalleryViewViewModel.init() {
        val isVideo = intent.getBooleanExtra(GalleryViewViewModel.IS_VIDEO, false)
        val gallery =
            intent.getStringExtra(GalleryViewViewModel.GALLERY) ?: R.string.all_gallery.getString()
        initGalleryData(gallery, isVideo)

        val mediaIndex = getMediaIndex()
        initViewPager(mediaIndex, galleryMedias) { position ->
            curPosition = position
            setMediaInfo(position)
        }
        setMediaInfo(mediaIndex)
        setInfo()

        onViewEvent {
            when (it) {
                GalleryViewViewModel.GalleryViewEvent.SetNote -> setInfo()
                GalleryViewViewModel.GalleryViewEvent.Share -> prepareSharedMedia()?.let { path ->
                    startActivityForResult(Intent(Intent.ACTION_SEND).apply {
                        putExtra(
                            Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                this@GalleryViewActivity,
                                "com.protone.seenn.fileProvider",
                                File(path)
                            )
                        )
                        type = "image/*"
                    }).let {
                        deleteSharedMedia(path)
                    }
                }
            }
        }
    }

    private suspend fun GalleryViewViewModel.setInfo() = withContext(Dispatchers.Default) {
        val galleryMedia = getSignedMedia()
        removeCato()
        galleryMedia?.cate?.onEach {

        }
        setNotes(getNotesWithGallery(galleryMedia?.uri ?: Uri.EMPTY))
    }

    private fun GalleryViewViewModel.setMediaInfo(position: Int) =
        galleryMedias[position].let { m ->
            setMediaInfo(
                m.name,
                m.date.toDateString("yyyy/MM/dd").toString(),
                m.size.getStorageSize(),
                m.path ?: m.uri.toString()
            )
        }

    private suspend fun GalleryViewViewModel.getMediaIndex() = onResult { co ->
        val galleryMedia =
            intent.getStringExtra(GalleryViewViewModel.MEDIA)?.toEntity(GalleryMedia::class.java)
        val indexOf = galleryMedias.indexOf(galleryMedia)
        curPosition = indexOf
        co.resumeWith(Result.success(indexOf))

    }

    private fun initViewPager(
        position: Int,
        data: MutableList<GalleryMedia>,
        onChange: (Int) -> Unit
    ) {
        binding.galleryVView.apply {
            val onSingleClick = {
                binding.galleryVCover.isVisible = !binding.galleryVCover.isVisible
            }
            adapter = object : FragmentStateAdapter(this@GalleryViewActivity) {
                override fun getItemCount(): Int = data.size
                override fun getItemViewType(position: Int): Int = position
                override fun createFragment(position: Int): Fragment =
                    GalleryViewFragment(data[position], onSingleClick)
            }
            binding.galleryVCover.isVisible = false

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    onChange.invoke(position)
                    super.onPageSelected(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCREEN_STATE_OFF) sendViewEvent(GalleryViewViewModel.GalleryViewEvent.SetNote)
                }
            })
            setCurrentItem(position, false)
        }
    }

    private fun setMediaInfo(
        title: String,
        time: String,
        size: String,
        type: String,
    ) = binding.run {
        galleryVTitle.text = title
        galleryVTime.text = String.format(
            R.string.time.getString(),
            time.ifEmpty { R.string.none.getString() })
        galleryVSize.text = String.format(
            R.string.size.getString(),
            size.ifEmpty { R.string.none.getString() })
        galleryVType.text = String.format(
            R.string.location.getString(),
            type.ifEmpty { R.string.none.getString() })
    }

    private suspend fun setNotes(notes: MutableList<String>) = withContext(Dispatchers.Main) {
        binding.galleryVLinks.isGone = notes.isEmpty()
        if (binding.galleryVLinks.adapter is CheckListAdapter)
            (binding.galleryVLinks.adapter as CheckListAdapter).dataList = notes
    }

    private suspend fun removeCato() = withContext(Dispatchers.Main) {
        binding.galleryVCatoContainer.removeAllViews()
    }

    fun showPop() {
        showPop(binding.galleryVAction, false)
    }

    override fun popDelete() {
        tryDelete(mutableListOf(viewModel.getCurrentMedia())) {
            if (it.size == 1) {
                val index = viewModel.galleryMedias.indexOf(it[0])
                if (index != -1) {
                    viewModel.galleryMedias.removeAt(index)
                    binding.galleryVView.adapter?.notifyItemRemoved(index)
                }
            }
        }
    }

    override fun popMoveTo() {
        launch {
            moveTo(
                binding.galleryVAction,
                viewModel.getCurrentMedia().isVideo,
                mutableListOf(viewModel.getCurrentMedia())
            ) { _, _ -> }
        }
    }

    override fun popRename() {
        tryRename(mutableListOf(viewModel.getCurrentMedia()))
    }

    override fun popSelectAll() = Unit

    override fun popSetCate() {
        addCate(mutableListOf(viewModel.getCurrentMedia()))
    }

    override fun popIntoBox() = Unit
}