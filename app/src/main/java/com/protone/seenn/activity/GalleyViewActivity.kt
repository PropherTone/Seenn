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
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toEntity
import com.protone.api.onResult
import com.protone.seenn.R
import com.protone.seenn.databinding.GalleyViewActivityBinding
import com.protone.seenn.fragment.GalleyViewFragment
import com.protone.ui.adapter.CatoListAdapter
import com.protone.ui.adapter.CheckListAdapter
import com.protone.worker.viewModel.GalleyViewViewModel
import com.protone.worker.viewModel.NoteViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleyViewActivity : BaseMediaActivity<
        GalleyViewActivityBinding,
        GalleyViewViewModel,
        GalleyViewViewModel.GalleyViewEvent>(true) {
    override val viewModel: GalleyViewViewModel by viewModels()

    override fun createView(): GalleyViewActivityBinding {
        return GalleyViewActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@GalleyViewActivity
            fitStatuesBarUsePadding(galleyVCover)
            initPop()
            popLayout?.galleyIntoBox?.isGone = true
            popLayout?.galleySelectAll?.isGone = true
            next.setOnClickListener {
                galleyVView.setCurrentItem(1 + galleyVView.currentItem, true)
            }
            previous.setOnClickListener {
                galleyVView.setCurrentItem(galleyVView.currentItem - 1, true)
            }
            galleyVLinks.apply {
                adapter = CheckListAdapter(this@GalleyViewActivity, check = false).also {
                    it.startNote = {
                        startActivity(NoteViewActivity::class.intent.apply {
                            putExtra(NoteViewViewModel.NOTE_NAME, it)
                        })
                    }
                }
                layoutManager = LinearLayoutManager(context)
            }
            galleyVCatoContainer.apply {
                adapter = CatoListAdapter(this@GalleyViewActivity,
                    object : CatoListAdapter.CatoListDataProxy {
                        override fun getMedia(): GalleyMedia {
                            return viewModel.getCurrentMedia()
                        }
                    })
                layoutManager = LinearLayoutManager(context).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
        }
    }

    override suspend fun GalleyViewViewModel.init() {
        val isVideo = intent.getBooleanExtra(GalleyViewViewModel.TYPE, false)
        val galley =
            intent.getStringExtra(GalleyViewViewModel.GALLEY) ?: R.string.all_galley.getString()
        initGalleyData(galley, isVideo)

        val mediaIndex = getMediaIndex()
        initViewPager(mediaIndex, galleyMedias) { position ->
            curPosition = position
            setMediaInfo(position)
        }
        setMediaInfo(mediaIndex)
        setInfo()

        onViewEvent {
            when (it) {
                GalleyViewViewModel.GalleyViewEvent.SetNote -> setInfo()
                GalleyViewViewModel.GalleyViewEvent.Share -> prepareSharedMedia()?.let { path ->
                    startActivityForResult(Intent(Intent.ACTION_SEND).apply {
                        putExtra(
                            Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                this@GalleyViewActivity,
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

    private suspend fun GalleyViewViewModel.setInfo() = withContext(Dispatchers.Default) {
        val galleyMedia = getSignedMedia()
        removeCato()
        galleyMedia?.cate?.onEach {

        }
        setNotes(getNotesWithGalley(galleyMedia?.uri ?: Uri.EMPTY))
    }

    private fun GalleyViewViewModel.setMediaInfo(position: Int) = galleyMedias[position].let { m ->
        setMediaInfo(
            m.name,
            m.date.toDateString("yyyy/MM/dd").toString(),
            m.size.getStorageSize(),
            m.path ?: m.uri.toString()
        )
    }

    private suspend fun GalleyViewViewModel.getMediaIndex() = onResult { co ->
        val galleyMedia =
            intent.getStringExtra(GalleyViewViewModel.MEDIA)?.toEntity(GalleyMedia::class.java)
        val indexOf = galleyMedias.indexOf(galleyMedia)
        curPosition = indexOf
        co.resumeWith(Result.success(indexOf))

    }

    private fun initViewPager(
        position: Int,
        data: MutableList<GalleyMedia>,
        onChange: (Int) -> Unit
    ) {
        binding.galleyVView.apply {
            val onSingleClick = {
                binding.galleyVCover.isVisible = !binding.galleyVCover.isVisible
            }
            adapter = object : FragmentStateAdapter(this@GalleyViewActivity) {
                override fun getItemCount(): Int = data.size
                override fun getItemViewType(position: Int): Int = position
                override fun createFragment(position: Int): Fragment =
                    GalleyViewFragment(data[position], onSingleClick)
            }
            binding.galleyVCover.isVisible = false

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    onChange.invoke(position)
                    super.onPageSelected(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCREEN_STATE_OFF) sendViewEvent(GalleyViewViewModel.GalleyViewEvent.SetNote)
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
        galleyVTitle.text = title
        galleyVTime.text = String.format(
            R.string.time.getString(),
            time.ifEmpty { R.string.none.getString() })
        galleyVSize.text = String.format(
            R.string.size.getString(),
            size.ifEmpty { R.string.none.getString() })
        galleyVType.text = String.format(
            R.string.location.getString(),
            type.ifEmpty { R.string.none.getString() })
    }

    private suspend fun setNotes(notes: MutableList<String>) = withContext(Dispatchers.Main) {
        binding.galleyVLinks.isGone = notes.isEmpty()
        if (binding.galleyVLinks.adapter is CheckListAdapter)
            (binding.galleyVLinks.adapter as CheckListAdapter).dataList = notes
    }

    private suspend fun removeCato() = withContext(Dispatchers.Main) {
        binding.galleyVCatoContainer.removeAllViews()
    }

    fun showPop() {
        showPop(binding.galleyVAction, false)
    }

    override fun popDelete() {
        tryDelete(mutableListOf(viewModel.getCurrentMedia())) {
            if (it.size == 1) {
                val index = viewModel.galleyMedias.indexOf(it[0])
                if (index != -1) {
                    viewModel.galleyMedias.removeAt(index)
                    binding.galleyVView.adapter?.notifyItemRemoved(index)
                }
            }
        }
    }

    override fun popMoveTo() {
        launch {
            moveTo(
                binding.galleyVAction,
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