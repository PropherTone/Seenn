package com.protone.seenn.fragment

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.onGlobalLayout
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toJson
import com.protone.seenn.R
import com.protone.seenn.activity.GalleySearchActivity
import com.protone.seenn.activity.GalleyViewActivity
import com.protone.seenn.activity.PictureBoxActivity
import com.protone.ui.adapter.BaseAdapter
import com.protone.ui.adapter.GalleyBucketAdapter
import com.protone.ui.adapter.GalleyListAdapter
import com.protone.ui.customView.StatusImageView
import com.protone.ui.databinding.GalleyBucketListLayoutBinding
import com.protone.ui.databinding.GalleyFragmentLayoutBinding
import com.protone.ui.dialog.titleDialog
import com.protone.ui.itemDecoration.GalleyItemDecoration
import com.protone.worker.IntentDataHolder
import com.protone.worker.viewModel.GalleyFragmentViewModel
import com.protone.worker.viewModel.GalleyViewViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect

class GalleyFragment(
    private val isVideo: Boolean,
    private val isLock: Boolean,
    private val combine: Boolean,
    private val onAttach: (MutableSharedFlow<GalleyFragmentViewModel.FragEvent>) -> Unit
) : Fragment(), CoroutineScope by MainScope(),
    GalleyListAdapter.OnSelect {

    private lateinit var viewModel: GalleyFragmentViewModel

    private lateinit var binding: GalleyFragmentLayoutBinding

    private var onSelectMod = false
        set(value) {
            launch {
                binding.galleyToolButton.isVisible = value
            }
            field = value
        }

    private lateinit var toolButtonAnimator: ValueAnimator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val model: GalleyFragmentViewModel by viewModels()
        viewModel = model
        viewModel.apply {
            onAttach.invoke(fragFlow)
            launch(Dispatchers.Default) {
                fragFlow.buffer().collect {
                    when (it) {
                        is GalleyFragmentViewModel.FragEvent.AddBucket -> {
                            insertNewMedias(it.name, it.list)
                        }
                        is GalleyFragmentViewModel.FragEvent.SelectAll -> {
                            getListAdapter().selectAll()
                            onSelectMod = true
                        }
                        is GalleyFragmentViewModel.FragEvent.OnActionBtn -> {
                            onSelectMod = true
                        }
                        is GalleyFragmentViewModel.FragEvent.IntoBox -> {
                            IntentDataHolder.put(
                                (if (getListAdapter().selectList.size > 0) {
                                    getListAdapter().selectList
                                } else {
                                    getGalley(getGalleyName())
                                        ?: getGalley(R.string.all_galley.getString())
                                })
                            )
                            startActivity(PictureBoxActivity::class.intent)
                        }
                        is GalleyFragmentViewModel.FragEvent.OnNewBucket -> {
                            insertBucket(it.pairs)
                        }
                        is GalleyFragmentViewModel.FragEvent.OnMediaDeleted -> {
                            refreshBucket(it.galleyMedia)
                            if (onTargetGalley(it.galleyMedia.bucket)) {
                                noticeListUpdate(
                                    it.galleyMedia,
                                    GalleyListAdapter.MediaStatus.DELETED
                                )
                            }
                        }
                        is GalleyFragmentViewModel.FragEvent.OnMediaInserted -> {
                            refreshBucket(it.galleyMedia)
                            if (onTargetGalley(it.galleyMedia.bucket)) {
                                noticeListUpdate(
                                    it.galleyMedia,
                                    GalleyListAdapter.MediaStatus.INSERTED
                                )
                            }
                        }
                        is GalleyFragmentViewModel.FragEvent.OnMediaUpdated -> {
                            if (onTargetGalley(it.galleyMedia.bucket)) {
                                noticeListUpdate(
                                    it.galleyMedia,
                                    GalleyListAdapter.MediaStatus.UPDATED
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }
            isVideo = this@GalleyFragment.isVideo
            isLock = this@GalleyFragment.isLock
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GalleyFragmentLayoutBinding.inflate(inflater, container, false).apply {
            root.onGlobalLayout {
                galleyBucketContainer.botBlock = tabController.measuredHeight.toFloat()
                toolButtonAnimator = AnimationHelper.rotation(galleyToolButton, 45f)
                galleyShowBucket.setOnStateListener(object : StatusImageView.StateListener {
                    override fun onActive() {
                        viewModel.isBucketShowUp = true
                        galleyToolButton.isVisible = true
                        galleyBucketContainer.show()
                        toolButtonAnimator.reverse()
                    }

                    override fun onNegative() {
                        if (viewModel.rightGalley == "") {
                            binding.galleyBucket.findViewHolderForLayoutPosition(0)?.let {
                                if (it is BaseAdapter.Holder<*> && it.binding is GalleyBucketListLayoutBinding) {
                                    (it.binding as GalleyBucketListLayoutBinding).bucket.performClick()
                                    return
                                }
                            }
                        }
                        viewModel.isBucketShowUp = false
                        galleyToolButton.isVisible = onSelectMod
                        galleyBucketContainer.hide()
                        toolButtonAnimator.start()
                    }
                })
            }
            galleySearch.setOnClickListener {
                val galley = viewModel.getGalleyName()
                IntentDataHolder.put(viewModel.getGalley(galley))
                startActivity(GalleySearchActivity::class.intent.also { intent ->
                    intent.putExtra("GALLEY", galley)
                })
            }
            galleyToolButton.setOnClickListener {
                if (!viewModel.isBucketShowUp) {
                    (galleyList.adapter as GalleyListAdapter).quitSelectMod()
                    onSelectMod = false
                } else {
                    activity?.titleDialog(R.string.user_name.getString(), "") {
                        if (it.isNotEmpty()) viewModel.addBucket(it)
                        else R.string.enter.getString().toast()
                    }
                }
            }
        }
        initList()
        viewModel.sortData(combine)
        return binding.root
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    private fun initList() = binding.run {
        galleyList.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = GalleyListAdapter(context, true).also {
                it.multiChoose = true
                it.setOnSelectListener(this@GalleyFragment)
            }
            addItemDecoration(GalleyItemDecoration(paddingEnd))
        }
        galleyBucket.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GalleyBucketAdapter(
                context,
                object : GalleyBucketAdapter.GalleyBucketAdapterDataProxy {
                    override fun deleteGalleyBucket(bucket: String) {
                        viewModel.deleteGalleyBucket(bucket)
                    }
                }
            ) {
                binding.galleyShowBucket.negative()
                viewModel.rightGalley = it
                noticeListUpdate(viewModel.getGalley(it))
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    if (parent.getChildLayoutPosition(view) != 0) {
                        outRect.top = resources.getDimension(R.dimen.icon_padding).toInt() shr 1
                    }
                    super.getItemOffsets(outRect, view, parent, state)
                }
            })
        }
    }

    private fun insertBucket(pairs: Pair<Uri, Array<String>>) {
        getBucketAdapter().insertBucket(pairs)
    }

    private fun refreshBucket(media: GalleyMedia): Unit = viewModel.run {
        Pair(
            if ((getGalley(media.bucket)?.size ?: 0) > 0) {
                getGalley(media.bucket)?.get(0)?.uri ?: Uri.EMPTY
            } else Uri.EMPTY,
            arrayOf(
                media.bucket,
                (getGalley(media.bucket)?.size ?: 0).toString()
            )
        ).apply { getBucketAdapter().refreshBucket(this) }

        val all = R.string.all_galley.getString()
        Pair(
            if ((getGalley(all)?.size ?: 0) > 0) {
                getGalley(all)?.get(0)?.uri ?: Uri.EMPTY
            } else Uri.EMPTY,
            arrayOf(
                all,
                (getGalley(all)?.size ?: 0).toString()
            )
        ).apply { getBucketAdapter().refreshBucket(this) }
    }

    private fun noticeListUpdate(media: GalleyMedia, status: GalleyListAdapter.MediaStatus) {
        if (viewModel.isBucketShowUp) return
        launch {
            when (status) {
                GalleyListAdapter.MediaStatus.INSERTED -> {
                    getListAdapter().noticeListItemInsert(media)
                }
                GalleyListAdapter.MediaStatus.DELETED -> {
                    getListAdapter().noticeListItemDelete(media)
                }
                GalleyListAdapter.MediaStatus.UPDATED -> {
                    getListAdapter().noticeListItemUpdate(media)
                }
            }
        }
    }

    private fun noticeListUpdate(data: MutableList<GalleyMedia>?) {
        if (viewModel.isBucketShowUp) return
        launch {
            binding.galleyList.swapAdapter(GalleyListAdapter(requireContext(), true).also {
                data?.let { medias -> it.setMedias(medias) }
                it.multiChoose = true
                it.setOnSelectListener(this@GalleyFragment)
            }, false)
        }
    }

    private fun getBucketAdapter() =
        binding.galleyBucket.adapter as GalleyBucketAdapter

    private fun getListAdapter() =
        binding.galleyList.adapter as GalleyListAdapter

    override fun select(galleyMedia: GalleyMedia) = Unit

    override fun select(galleyMedia: MutableList<GalleyMedia>) {
        launch {
            viewModel.sendEvent(GalleyFragmentViewModel.FragEvent.OnSelect(galleyMedia))
        }
    }

    override fun openView(galleyMedia: GalleyMedia) {
        startActivity(GalleyViewActivity::class.intent.apply {
            putExtra(GalleyViewViewModel.MEDIA, galleyMedia.toJson())
            putExtra(GalleyViewViewModel.TYPE, galleyMedia.isVideo)
            putExtra(GalleyViewViewModel.GALLEY, viewModel.getGalleyName())
        })
    }

}
