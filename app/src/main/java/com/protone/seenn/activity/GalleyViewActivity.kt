package com.protone.seenn.activity

import android.net.Uri
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.protone.api.baseType.getStorageSize
import com.protone.api.baseType.getString
import com.protone.api.baseType.toDateString
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.json.toUri
import com.protone.api.onResult
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.adapter.GalleyViewPager2Adapter
import com.protone.seen.databinding.ImageCateLayoutBinding
import com.protone.seen.databinding.TextCateLayoutBinding
import com.protone.seenn.R
import com.protone.seenn.databinding.GalleyViewActivityBinding
import com.protone.seenn.fragment.GalleyViewFragment
import com.protone.seenn.viewModel.GalleyViewViewModel
import com.protone.seenn.viewModel.NoteViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleyViewActivity :
    BaseMediaActivity<GalleyViewActivityBinding, GalleyViewViewModel>(true) {
    override val viewModel: GalleyViewViewModel by viewModels()

    override fun createView(): View {
        binding = GalleyViewActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBarUsePadding(binding.galleyVCover)
        initPop()
        popLayout?.galleyIntoBox?.isGone = true
        popLayout?.galleySelectAll?.isGone = true
        return binding.root
    }

    override suspend fun GalleyViewViewModel.init() {
        val isVideo = intent.getBooleanExtra(GalleyViewViewModel.TYPE, false)
        val galley =
            intent.getStringExtra(GalleyViewViewModel.GALLEY) ?: R.string.all_galley.getString()
        initGalleyData(galley, isVideo)
        init(isVideo)

        onViewEvent {
            when (it) {
                GalleyViewViewModel.GalleyViewEvent.SetNote -> viewModel.setInfo()
                else -> {}
            }
        }
    }

    private suspend fun GalleyViewViewModel.init(isVideo: Boolean) {
        initList {
            startActivity(NoteViewActivity::class.intent.apply {
                putExtra(NoteViewViewModel.NOTE_NAME, it)
            })
        }
        val mediaIndex = getMediaIndex()
        initViewPager(mediaIndex, galleyMedias, isVideo) { position ->
            curPosition = position
            setMediaInfo(position)
        }
        setMediaInfo(mediaIndex)
        setInfo()
    }

    private suspend fun GalleyViewViewModel.setInfo() {
        val galleyMedia = getSignedMedia()
        removeCato()
        galleyMedia?.cate?.onEach {
            withContext(Dispatchers.IO) {
                if (it.contains("content://")) {
                    val mediaByUri = getMediaByUri(it.toUri())
                    addCato(
                        withContext(Dispatchers.Main) {
                            ImageCateLayoutBinding.inflate(
                                layoutInflater,
                                root,
                                false
                            ).apply {
                                Glide.with(this@GalleyViewActivity).asDrawable().load(it.toUri())
                                    .into(catoBack)
                                catoName.text = mediaByUri?.name
                                root.setOnClickListener {
                                    startActivity(GalleyViewActivity::class.intent.apply {
                                        putExtra(GalleyViewViewModel.MEDIA, mediaByUri?.toJson())
                                        putExtra(GalleyViewViewModel.TYPE, mediaByUri?.isVideo)
                                    })
                                }
                            }.root
                        }

                    )
                } else {
                    addCato(
                        withContext(Dispatchers.Main) {
                            TextCateLayoutBinding.inflate(
                                layoutInflater,
                                root,
                                false
                            ).apply {
                                cato.text = it
                            }.root
                        }
                    )
                }
            }
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
        isVideo: Boolean = false,
        onChange: (Int) -> Unit
    ) {
        binding.galleyVView.apply {
            if (!isVideo) {
                adapter = GalleyViewPager2Adapter(context, data).also { a ->
                    a.onClk = {
                        binding.galleyVCover.isVisible = !binding.galleyVCover.isVisible
                    }
                }
            } else {
                adapter = object : FragmentStateAdapter(context as AppCompatActivity) {
                    override fun getItemCount(): Int = data.size
                    override fun createFragment(position: Int): Fragment =
                        GalleyViewFragment(data[position])
                }
                binding.galleyVCover.isVisible = false
            }
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

    private fun initList(startNote: (String) -> Unit) {
        binding.galleyVLinks.apply {
            adapter = CheckListAdapter(context, check = false).also {
                it.startNote = startNote
            }
            layoutManager = LinearLayoutManager(context)
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

    private suspend fun addCato(view: View) = withContext(Dispatchers.Main) {
        binding.galleyVCatoContainer.addView(view)
    }

    private suspend fun removeCato() = withContext(Dispatchers.Main) {
        binding.galleyVCatoContainer.removeAllViews()
    }

    fun showPop() {
        showPop(binding.galleyVAction, false)
    }

    override fun popDelete() {
        tryDelete(mutableListOf(viewModel.getCurrentMedia())) {
//            binding.galleyVView.setCurrentItem(viewModel.curPosition + 1, true)
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