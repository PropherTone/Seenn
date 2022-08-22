package com.protone.seenn.fragment

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toJson
import com.protone.seen.adapter.GalleyBucketAdapter
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seen.customView.StatusImageView
import com.protone.seen.databinding.GalleyFragmentLayoutBinding
import com.protone.seen.dialog.titleDialog
import com.protone.seen.itemDecoration.GalleyItemDecoration
import com.protone.seenn.IntentDataHolder
import com.protone.seenn.R
import com.protone.seenn.activity.GalleySearchActivity
import com.protone.seenn.activity.GalleyViewActivity
import com.protone.seenn.activity.PictureBoxActivity
import com.protone.seenn.viewModel.GalleyFragmentViewModel
import com.protone.seenn.viewModel.GalleyViewViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect

class GalleyFragment(
    private val isVideo: Boolean,
    private val isLock: Boolean,
    private val combine : Boolean,
    private val onAttach: (MutableSharedFlow<GalleyFragmentViewModel.FragEvent>) -> Unit
) : Fragment(), CoroutineScope by MainScope(),
    ViewTreeObserver.OnGlobalLayoutListener,
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

    private lateinit var containerAnimator: ValueAnimator
    private lateinit var toolButtonAnimator: ValueAnimator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val model: GalleyFragmentViewModel by viewModels()
        viewModel = model
        viewModel.apply {
            onAttach.invoke(fragFlow)
            launch(Dispatchers.Default) {
                fragFlow.collect {
                    when (it) {
                        is GalleyFragmentViewModel.FragEvent.DeleteMedia -> {
                            if (!isLock) {
                                it.galleyMedia.type?.onEach { type ->
                                    if (galleyMap[type]?.remove(it.galleyMedia) == true) {
                                        getListAdapter().removeMedia(it.galleyMedia)
                                    }
                                }
                            }
                            if (galleyMap[it.galleyMedia.bucket]?.remove(it.galleyMedia) == true) {
                                getListAdapter().removeMedia(it.galleyMedia)
                            }
                            if (galleyMap[R.string.all_galley.getString()]?.remove(it.galleyMedia) == true) {
                                getListAdapter().removeMedia(it.galleyMedia)
                            }
                            refreshBucket(it.galleyMedia)
                        }
                        is GalleyFragmentViewModel.FragEvent.AddBucket -> {
                            if (!isLock) {
                                if (galleyMap[it.name] == null) {
                                    galleyMap[it.name] = mutableListOf()
                                }
                                galleyMap[it.name]?.addAll(it.list)
                            }
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
                                    galleyMap[rightGalley]
                                        ?: galleyMap[R.string.all_galley.getString()]
                                })
                            )
                            startActivity(PictureBoxActivity::class.intent)
                        }
                        is GalleyFragmentViewModel.FragEvent.OnGalleyUpdate -> {
                            updateGalley(it.media) { status, media ->
                                when (status) {
                                    GalleyMedia.MediaStatus.Updated -> {
                                        if (onTargetGalley(media.bucket)) {
                                            getListAdapter().noticeListItemUpdate(media)
                                        }
                                    }
                                    GalleyMedia.MediaStatus.Deleted -> {
                                        val index =
                                            viewModel.galleyMap[media.bucket]?.indexOf(media)
                                        if (index != null && index == 0) {
                                            refreshBucket(media)
                                        }
                                        if (onTargetGalley(media.bucket)) {
                                            getListAdapter().noticeListItemDelete(media)
                                        }
                                    }
                                    GalleyMedia.MediaStatus.NewInsert -> {
                                        refreshBucket(media)
                                        if (onTargetGalley(media.bucket)) {
                                            getListAdapter().noticeListItemInsert(media)
                                        }
                                    }
                                }
                            }
                        }
                        is GalleyFragmentViewModel.FragEvent.OnNewBucket -> {
                            insertBucket(it.pairs)
                        }
                        is GalleyFragmentViewModel.FragEvent.OnListUpdate -> {
                            getBucketAdapter().performSelect()
                            noticeListUpdate(it.data)
                        }
                        is GalleyFragmentViewModel.FragEvent.OnGetAllGalley -> {
                            getBucketAdapter().performSelect()
                            noticeListUpdate(viewModel.galleyMap[R.string.all_galley.getString()])
                        }
                        else -> {}
                    }
                }
            }
        }
        viewModel.rightGalley = R.string.all_galley.getString()
        viewModel.isVideo = isVideo
        viewModel.isLock = isLock
        viewModel.combine = combine
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GalleyFragmentLayoutBinding.inflate(inflater, container, false).apply {
            root.viewTreeObserver.addOnGlobalLayoutListener(this@GalleyFragment)
            galleySearch.setOnClickListener {
                IntentDataHolder.put(viewModel.galleyMap[viewModel.rightGalley])
                startActivity(GalleySearchActivity::class.intent.also { intent ->
                    intent.putExtra("GALLEY", viewModel.rightGalley)
                })
            }
            galleyToolButton.setOnClickListener {
                if (!viewModel.isBucketShowUp) {
                    (binding.galleyList.adapter as GalleyListAdapter).quitSelectMod()
                    onSelectMod = false
                } else {
                    activity?.titleDialog(R.string.user_name.getString(), "") {
                        if (it.isNotEmpty()) {
                            viewModel.addBucket(it)
                        } else R.string.enter.getString().toast()
                    }
                }
            }
        }
        initList()
        viewModel.apply {
            sortData()
            if (!isLock) sortPrivateData()
        }
        return binding.root
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    private fun start() {
        viewModel.isBucketShowUp = false
        binding.galleyToolButton.isVisible = onSelectMod
        containerAnimator.start()
        toolButtonAnimator.start()
    }

    private fun reverse() {
        viewModel.isBucketShowUp = true
        binding.galleyToolButton.isVisible = true
        containerAnimator.reverse()
        toolButtonAnimator.reverse()
    }

    private fun initList() = binding.run {
        galleyList.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = GalleyListAdapter(context,true).also {
                it.multiChoose = true
                it.setOnSelectListener(this@GalleyFragment)
            }
            addItemDecoration(GalleyItemDecoration(paddingEnd))
        }
        galleyBucket.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GalleyBucketAdapter(context) {
                noticeListUpdate(viewModel.galleyMap[it])
                binding.galleyShowBucket.negative()
                viewModel.rightGalley = it
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    if (parent.getChildLayoutPosition(view) != 0) {
                        outRect.top = resources.getDimension(R.dimen.icon_padding).toInt() shr 1
                    }
                }
            })
        }
    }

    override fun onGlobalLayout() {
        binding.galleyBucket.height.toFloat().let {
            containerAnimator = AnimationHelper.translationY(
                binding.galleyBucketContainer,
                it
            )
        }
        toolButtonAnimator = AnimationHelper.rotation(
            binding.galleyToolButton,
            45f
        )
        binding.galleyShowBucket.setOnStateListener(object : StatusImageView.StateListener {
            override fun onActive() {
                reverse()
            }

            override fun onNegative() {
                start()
            }
        })
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    private fun insertBucket(pairs: Pair<Uri, Array<String>>) {
        getBucketAdapter().insertBucket(pairs)
    }

    private fun refreshBucket(media: GalleyMedia) {
        Pair(
            if ((viewModel.galleyMap[media.bucket]?.size ?: 0) > 0)
                viewModel.galleyMap[media.bucket]?.get(0)?.uri ?: Uri.EMPTY
            else
                Uri.EMPTY,
            arrayOf(
                media.bucket,
                viewModel.galleyMap[media.bucket]?.size.toString()
            )
        ).apply { getBucketAdapter().refreshBucket(this) }
        val all = R.string.all_galley.getString()
        Pair(
            if ((viewModel.galleyMap[all]?.size ?: 0) > 0)
                viewModel.galleyMap[all]?.get(0)?.uri ?: Uri.EMPTY
            else
                Uri.EMPTY,
            arrayOf(
                all,
                viewModel.galleyMap[all]?.size.toString()
            )
        ).apply { getBucketAdapter().refreshBucket(this) }
    }

    private fun noticeListUpdate(data: MutableList<GalleyMedia>?) {
        getListAdapter().noticeDataUpdate(data)
    }

    private fun getBucketAdapter() =
        binding.galleyBucket.adapter as GalleyBucketAdapter

    private fun getListAdapter() =
        binding.galleyList.adapter as GalleyListAdapter

    override fun select(galleyMedia: GalleyMedia) = Unit

    override fun select(galleyMedia: MutableList<GalleyMedia>) {
        viewModel.sendEvent(GalleyFragmentViewModel.FragEvent.OnSelect(galleyMedia))
    }

    override fun openView(galleyMedia: GalleyMedia) {
        startActivity(GalleyViewActivity::class.intent.apply {
            putExtra(GalleyViewViewModel.MEDIA, galleyMedia.toJson())
            putExtra(GalleyViewViewModel.TYPE, galleyMedia.isVideo)
            putExtra(GalleyViewViewModel.GALLEY, viewModel.rightGalley)
        })
    }

}
