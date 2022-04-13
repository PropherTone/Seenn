package com.protone.seen

import android.animation.ValueAnimator
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.Config
import com.protone.api.animation.AnimationHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.GalleyBucketAdapter
import com.protone.seen.adapter.GalleyItemDecoration
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seen.customView.StateImageView
import com.protone.seen.databinding.GalleyFragmentLayoutBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlin.coroutines.suspendCoroutine

class GalleyFragment(
    private val mContext: FragmentActivity,
    private val galleyMediaList: MutableMap<String, MutableList<GalleyMedia>>,
    val live: MutableLiveData<MutableList<GalleyMedia>>,
    val multiChoose: Boolean = false,
    private val isVideo: Boolean = false
) : Fragment(),
    CoroutineScope by CoroutineScope(Dispatchers.Main),
    ViewTreeObserver.OnGlobalLayoutListener, SearchView.OnQueryTextListener {

    enum class Event {
        UpdateBucket,
        UpdateGalley
    }

    val channel = Channel<Event>(Channel.UNLIMITED)

    private lateinit var selectedBucket: String

    private var isSelectMod = false

    private var isBucketShowUp = false

    private lateinit var galleyView: GalleyFragmentLayoutBinding

    private lateinit var containerAnimator: ValueAnimator
    private lateinit var toolButtonAnimator: ValueAnimator
    private lateinit var searchAnimator: ValueAnimator

    private fun start() {
        isBucketShowUp = false
        galleyView.galleyToolButton.isVisible = isSelectMod
        containerAnimator.start()
        toolButtonAnimator.start()
    }

    private fun reverse() {
        isBucketShowUp = true
        galleyView.galleyToolButton.isVisible = true
        containerAnimator.reverse()
        toolButtonAnimator.reverse()
    }

    private fun searchShow() {
        galleyView.apply {
            searchAnimator = AnimationHelper.translationY(
                galleyBucketContainer,
                Config.keyboardHeight.toFloat(),
                play = true
            )
//            searchAnimator = ObjectAnimator.ofFloat(
//                galleyBucketContainer,
//                "translationY",
//                Config.keyboardHeight.toFloat()
//            )
            galleyToolButton.isVisible = false
            galleyShowBucket.isVisible = false
        }
    }

    private fun searchHide() {
        if (::searchAnimator.isInitialized) searchAnimator.reverse()
        galleyView.apply {
            galleyToolButton.isVisible = true
            galleyShowBucket.isVisible = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        selectedBucket = getString(R.string.all_galley)
        galleyView = GalleyFragmentLayoutBinding.inflate(inflater, container, false).apply {
            root.viewTreeObserver.addOnGlobalLayoutListener(this@GalleyFragment)
            galleyList.itemAnimator = null
            galleyBucket.itemAnimator = null
            galleySearch.apply {
                setOnQueryTextListener(this@GalleyFragment)
                setOnSearchClickListener {
                    searchShow()
                }
                setOnCloseListener {
                    searchHide()
                    false
                }
            }
        }
        return galleyView.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch {
            initList()
            while (isActive) {
                select<Unit> {
                    channel.onReceive {
                        when (it) {
                            Event.UpdateBucket -> updateBucket()
                            Event.UpdateGalley -> {
                                updateGalley(selectedBucket)
                                galleyView.galleyShowBucket.negative()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stateCheck() {
        galleyView.galleyShowBucket.setOnStateListener(object : StateImageView.StateListener {
            override fun onActive() {
                reverse()
            }

            override fun onNegative() {
                start()
            }
        })
    }

    private fun getGalleyAdapter(): GalleyListAdapter? {
        return initListData()?.run {
            this
        }?.let {
            GalleyListAdapter(mContext, it, isVideo) { lk ->
                isSelectMod = lk
                galleyView.galleyToolButton.isVisible = lk
                live.postValue(mutableListOf())
            }.apply {
                setOnSelectListener(object : GalleyListAdapter.OnSelect {
                    override fun select(galleyMedia: MutableList<GalleyMedia>) {
                        live.postValue(galleyMedia)
                    }
                })
            }
        }
    }

    private suspend fun initList() = withContext(Dispatchers.Main) {
        galleyView.galleyBucket.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = GalleyBucketAdapter(mContext, mutableListOf()) {
                selectedBucket = it
                channel.offer(Event.UpdateGalley)
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
        galleyView.galleyList.apply {
            layoutManager = GridLayoutManager(mContext, 4)
            adapter = getGalleyAdapter()?.apply {
                galleyView.galleyToolButton.setOnClickListener {
                    if (isBucketShowUp) {
                        addBucket()
                    } else {
                        quitSelectMod()
                    }
                }
                multiChoose = !this@GalleyFragment.multiChoose
            }
            addItemDecoration(GalleyItemDecoration(paddingEnd))
        }
        channel.offer(Event.UpdateBucket)
    }

    private fun addBucket() {
    }

    private suspend fun updateBucket() = withContext(Dispatchers.Main) {
        (galleyView.galleyBucket.adapter as GalleyBucketAdapter?)?.noticeDataUpdate(getBucketData())
    }

    private suspend fun updateGalley(name: String) = withContext(Dispatchers.Main) {
        (galleyView.galleyList.adapter as GalleyListAdapter?)?.noticeDataUpdate(getListData(name))
    }

    private suspend fun updateGalley(item: MutableList<GalleyMedia>) =
        withContext(Dispatchers.Main) {
            (galleyView.galleyList.adapter as GalleyListAdapter?)?.noticeDataUpdate(item)
        }

    override fun onGlobalLayout() {
        galleyView.galleyBucket.height.toFloat().let {
            containerAnimator = AnimationHelper.translationY(
                galleyView.galleyBucketContainer,
                it
            )
//            containerAnimator = ObjectAnimator.ofFloat(
//                galleyView.galleyBucketContainer,
//                "translationY",
//                it
//            )
        }
        toolButtonAnimator = AnimationHelper.rotation(
            galleyView.galleyToolButton,
            45f
        )
//        toolButtonAnimator = ObjectAnimator.ofFloat(
//            galleyView.galleyToolButton,
//            "rotation",
//            45f
//        )
        stateCheck()
        galleyView.root.viewTreeObserver?.removeOnGlobalLayoutListener(this)
    }

    private suspend fun mapList(
        mediaList: MutableMap<String, MutableList<GalleyMedia>>
    ): MutableList<Pair<Uri, Array<String>>> =
        withContext(Dispatchers.IO) {
            suspendCoroutine {
                val videoBucket = mutableListOf<Pair<Uri, Array<String>>>()
                mediaList.forEach { (s, mutableList) ->
                    if (mutableList.size > 0) {
                        videoBucket.add(
                            Pair(
                                mutableList[0].uri,
                                arrayOf(s, mutableList.size.toString())
                            )
                        )
                    }
                }
                it.resumeWith(Result.success(videoBucket))
            }
        }

    override fun onQueryTextSubmit(query: String?): Boolean {
        launch {
            updateGalley(filterData(selectedBucket, query.toString()))
        }
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean = false

    private suspend fun filterData(name: String, query: String): MutableList<GalleyMedia> =
        withContext(Dispatchers.IO) {
            getListData(name).filter { it.name.contains(query) } as MutableList<GalleyMedia>
        }


    private suspend fun getBucketData(): MutableList<Pair<Uri, Array<String>>> =
        mapList(galleyMediaList)

    private fun getListData(name: String): MutableList<GalleyMedia> =
        galleyMediaList[name] as MutableList<GalleyMedia>

    private fun initListData(): MutableList<GalleyMedia>? = galleyMediaList[selectedBucket]
}

//class PhotoFragment(
//    mContext: FragmentActivity,
//    private val photoMediaList: MutableMap<String, MutableList<GalleyMedia>>
//) : GalleyFragment(mContext) {
//
//    override suspend fun getBucketData(): MutableList<Pair<Uri, Array<String>>> =
//        mapList(photoMediaList)
//
//    override suspend fun getListData(name: String): MutableList<GalleyMedia> =
//        photoMediaList[name] as MutableList<GalleyMedia>
//
//    override suspend fun initListData(): MutableList<GalleyMedia>? = photoMediaList[selectedBucket]
//
//}
//
//class VideoFragment(
//    mContext: FragmentActivity,
//    private val videoMediaList: MutableMap<String, MutableList<GalleyMedia>>
//) : GalleyFragment(mContext) {
//
//    init {
//        isVideo = true
//    }
//
//    override suspend fun getBucketData(): MutableList<Pair<Uri, Array<String>>> =
//        mapList(videoMediaList)
//
//    override suspend fun getListData(name: String): MutableList<GalleyMedia> =
//        videoMediaList[name] as MutableList<GalleyMedia>
//
//    override suspend fun initListData(): MutableList<GalleyMedia>? = videoMediaList[selectedBucket]
//
//}
