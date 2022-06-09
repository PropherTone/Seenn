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
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.intent
import com.protone.api.context.onUiThread
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyBucket
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.media.FragMailer
import com.protone.mediamodle.media.IGalleyFragment
import com.protone.seen.adapter.GalleyBucketAdapter
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seen.customView.StateImageView
import com.protone.seen.databinding.GalleyFragmentLayoutBinding
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.itemDecoration.GalleyItemDecoration
import com.protone.seenn.GalleySearchActivity
import com.protone.seenn.R
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.*

class GalleyFragment(
    private val isVideo: Boolean,
    private val isLock: Boolean
) : Fragment(), CoroutineScope by MainScope(),
    ViewTreeObserver.OnGlobalLayoutListener,
    GalleyListAdapter.OnSelect {

    private lateinit var rightGalley: String
    private lateinit var binding: GalleyFragmentLayoutBinding

    private var onSelectMod = false
        set(value) {
            binding.galleyToolButton.isVisible = value
            field = value
        }

    private var isBucketShowUp = true

    private lateinit var containerAnimator: ValueAnimator
    private lateinit var toolButtonAnimator: ValueAnimator

    private val galleyMap = mutableMapOf<String?, MutableList<GalleyMedia>>()

    var iGalleyFragment: IGalleyFragment? = null
    var fragMailer: FragMailer = object : FragMailer {

        override fun deleteMedia(galleyMedia: GalleyMedia) {
            if (!isLock) {
                galleyMedia.type?.onEach {
                    if (galleyMap[it]?.remove(galleyMedia) == true) {
                        getListAdapter()?.removeMedia(galleyMedia)
                    }
                }
            }
            if (galleyMap[galleyMedia.bucket]?.remove(galleyMedia) == true) {
                getListAdapter()?.removeMedia(galleyMedia)
            }
        }

        override fun addBucket(name: String, list: MutableList<GalleyMedia>) {
            if (!isLock) {
                if (galleyMap[name] == null) {
                    galleyMap[name] = mutableListOf()
                }
                galleyMap[name]?.addAll(list)
            }
        }

        override fun selectAll() {
            getListAdapter()?.selectAll()
            onSelectMod = true
        }

        override fun onActionBtn() {
            onSelectMod = true
        }

        override fun getChooseGalley(): MutableList<GalleyMedia>? {
            return galleyMap[rightGalley] ?: galleyMap[context?.getString(R.string.all_galley)]
        }

    }

    private fun start() {
        isBucketShowUp = false
        binding.galleyToolButton.isVisible = onSelectMod
        containerAnimator.start()
        toolButtonAnimator.start()
    }

    private fun reverse() {
        isBucketShowUp = true
        binding.galleyToolButton.isVisible = true
        containerAnimator.reverse()
        toolButtonAnimator.reverse()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        rightGalley = context.getString(R.string.all_galley)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GalleyFragmentLayoutBinding.inflate(inflater, container, false).apply {
            root.viewTreeObserver.addOnGlobalLayoutListener(this@GalleyFragment)
            galleySearch.setOnClickListener {
                IntentDataHolder.put(galleyMap[rightGalley])
                context?.startActivity(GalleySearchActivity::class.intent.also { intent ->
                    intent.putExtra("GALLEY", rightGalley)
                })
            }
            galleyToolButton.setOnClickListener {
                if (!isBucketShowUp) {
                    if (onSelectMod && binding.galleyList.adapter is GalleyListAdapter) {
                        (binding.galleyList.adapter as GalleyListAdapter).quitSelectMod()
                        onSelectMod = false
                    }
                } else {
                    TitleDialog(
                        requireContext(),
                        requireContext().getString(R.string.user_name),
                        ""
                    ) {
                        if (it.isNotEmpty()) {
                            addBucket(it)
                        } else toast(requireContext().getString(R.string.enter))
                    }
                }
            }
        }
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch(Dispatchers.IO) {
            while (true) {
                if (::binding.isInitialized) {
                    launch(Dispatchers.Main) {
                        initList()
                        sortData()
                        if (!isLock) sortPrivateData()
                    }
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private fun initList() = binding.run {
        galleyList.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = GalleyListAdapter(context, mutableListOf(), isVideo).also {
                it.multiChoose = true
                it.setOnSelectListener(this@GalleyFragment)
            }
            addItemDecoration(GalleyItemDecoration(paddingEnd))
        }
        galleyBucket.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GalleyBucketAdapter(context, mutableListOf()) {
                binding.galleyShowBucket.negative()
                rightGalley = it
                noticeListUpdate(galleyMap[it])
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
                        outRect.top =
                            resources.getDimension(R.dimen.icon_padding)
                                .toInt() shr 1
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
        binding.galleyShowBucket.setOnStateListener(object : StateImageView.StateListener {
            override fun onActive() {
                reverse()
            }

            override fun onNegative() {
                start()
            }
        })
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    private fun sortPrivateData() = launch(Dispatchers.IO) {
        (DataBaseDAOHelper.getALLGalleyBucket(isVideo) as MutableList<GalleyBucket>?)?.forEach {
            synchronized(binding) {
                insertBucket(Pair(Uri.EMPTY, arrayOf(it.type, "PRIVATE")))
                galleyMap[it.type] = mutableListOf()
            }
        }
    }

    private fun sortData() = launch(Dispatchers.IO) {
        galleyMap[context?.getString(R.string.all_galley)] = mutableListOf()
        DataBaseDAOHelper.run {
            val signedMedias = getAllMediaByType(isVideo) as MutableList<GalleyMedia>?
            if (signedMedias == null) {
                toast(requireContext().getString(R.string.none))
                return@launch
            }
            signedMedias.let {
                galleyMap[context?.getString(R.string.all_galley)] = it
                insertBucket(
                    Pair(
                        if (signedMedias.size > 0) signedMedias[0].uri else Uri.EMPTY,
                        arrayOf(
                            requireContext().getString(R.string.all_galley),
                            signedMedias.size.toString()
                        )
                    )
                )
                getBucketAdapter()?.performSelect()
                noticeListUpdate(it)
            }
            if (!isLock) launch(Dispatchers.IO) {
                signedMedias.forEach {
                    it.type?.forEach { type ->
                        if (galleyMap[type] == null) {
                            galleyMap[type] = mutableListOf()
                        }
                        galleyMap[type]?.add(it)
                    }
                }
            }
            getAllGalley(isVideo)?.forEach {
                galleyMap[it] = (getAllMediaByGalley(it, isVideo) as MutableList<GalleyMedia>)
                    .also { list ->
                        synchronized(binding) {
                            insertBucket(
                                Pair(
                                    if (list.size > 0) list[0].uri else Uri.EMPTY,
                                    arrayOf(it, list.size.toString())
                                )
                            )
                        }
                    }
            }

        }
    }

    private fun addBucket(name: String) {
        DataBaseDAOHelper.insertGalleyBucketCB(GalleyBucket(name, isVideo)) { re, reName ->
            if (re) {
                if (!isLock) {
                    insertBucket(Pair(Uri.EMPTY, arrayOf(reName, "PRIVATE")))
                    galleyMap[reName] = mutableListOf()
                } else {
                    toast(requireContext().getString(R.string.locked))
                }
            } else {
                toast(requireContext().getString(R.string.failed_msg))
            }
        }
    }

    private fun insertBucket(pairs: Pair<Uri, Array<String>>) {
        getBucketAdapter()?.insertBucket(pairs)
    }

    private fun noticeListUpdate(data: MutableList<GalleyMedia>?) {
        getListAdapter()?.noticeDataUpdate(data)
    }

    private fun getBucketAdapter() =
        if (binding.galleyBucket.adapter is GalleyBucketAdapter)
            (binding.galleyBucket.adapter as GalleyBucketAdapter) else null

    private fun getListAdapter() =
        if (binding.galleyList.adapter is GalleyListAdapter)
            binding.galleyList.adapter as GalleyListAdapter else null

    override fun select(galleyMedia: MutableList<GalleyMedia>) {
        iGalleyFragment?.select(galleyMedia)
    }

    override fun openView(galleyMedia: GalleyMedia) {
        iGalleyFragment?.openView(galleyMedia, rightGalley)
    }

    private fun toast(text: String) = requireContext().run {
        onUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }
}
