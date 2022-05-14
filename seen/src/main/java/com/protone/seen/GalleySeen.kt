package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.MyFragmentStateAdapter
import com.protone.seen.databinding.GalleyLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleySeen(context: Context) : PopupCoverSeen<GalleySeen.Touch>(context),
    TabLayout.OnTabSelectedListener {

    enum class Touch {
        Finish,
        DELETE,
        RENAME,
        MOVE_TO,
        SELECT_ALL,
        ADD_CATE,
        IntoBOX,
        ConfirmChoose,
        ShowPop
    }

    companion object {
        @JvmStatic
        val CHOOSE_PHOTO = "PHOTO"

        @JvmStatic
        val CHOOSE_VIDEO = "VIDEO"
    }

    private val binding = GalleyLayoutBinding.inflate(context.layoutInflater, context.root, false)

    private val mailers = arrayOfNulls<GalleyFragment.FragMailer>(2)
    private var rightMailer = 0

    val chooseData: MutableLiveData<MutableList<GalleyMedia>> =
        MutableLiveData<MutableList<GalleyMedia>>()

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.view

    fun getBar() = binding.galleyBar

    init {
        setSettleToolBar()
        binding.self = this
    }

    private fun initViewMode(chooseMode: Boolean) {
        if (chooseMode) {
            binding.galleyChooseConfirm.isGone = !chooseMode
            binding.galleyChooseConfirm.setOnClickListener { offer(Touch.ConfirmChoose) }
        }
    }

    override fun offer(event: Touch) {
        viewEvent.offer(event)
    }

    suspend fun initPager(
        galleyMediaList: MutableMap<String, MutableList<GalleyMedia>>,
        videoMediaList: MutableMap<String, MutableList<GalleyMedia>>,
        chooseType: String = "",
        openView: (GalleyMedia, Boolean) -> Unit,
        addBucket: (Boolean) -> Unit
    ) = withContext(Dispatchers.Main) {
        initViewMode(chooseType.isNotEmpty())
        binding.galleyPager.let {
            val photoFragment by lazy {
                GalleyFragment(
                    context as FragmentActivity,
                    galleyMediaList,
                    chooseData,
                    multiChoose = chooseType.isNotEmpty(),
                    openView = openView
                ).also { gf ->
                    gf.addBucket = addBucket
                    mailers[0] = gf.fragMailer
                }
            }
            val videoFragment by lazy {
                GalleyFragment(
                    context as FragmentActivity,
                    videoMediaList,
                    chooseData,
                    multiChoose = chooseType.isNotEmpty(),
                    isVideo = true,
                    openView = openView
                ).also { gf ->
                    gf.addBucket = addBucket
                    mailers[1] = gf.fragMailer
                }
            }
            it.adapter = MyFragmentStateAdapter(
                context as FragmentActivity,
                when (chooseType) {
                    CHOOSE_PHOTO -> arrayListOf(photoFragment)
                    CHOOSE_VIDEO -> arrayListOf(videoFragment)
                    else -> arrayListOf(photoFragment, videoFragment)
                }
            )
            TabLayoutMediator(binding.galleyTab.apply {
                addOnTabSelectedListener(this@GalleySeen)
            }, it) { tab, position ->
                tab.setText(
                    when (chooseType) {
                        CHOOSE_PHOTO -> arrayOf(R.string.photo)
                        CHOOSE_VIDEO -> arrayOf(R.string.video)
                        else -> arrayOf(R.string.photo, R.string.video)
                    }[position]
                )
            }.attach()
        }
    }

    fun showPop(isSelect : Boolean) {
        showPop(binding.galleyActionMenu,!isSelect)
    }

    fun refreshGalleries(galleries : MutableList<String>){

    }

    fun refreshMedias(medias : MutableList<GalleyMedia>){

    }

    fun addBucket(name: String) {
        mailers[rightMailer]?.addBucket(name)
    }

    fun selectAll(){
        mailers[rightMailer]?.selectAll()
    }

    fun hideActionBtn(){
        binding.galleyActionMenu.isVisible = false
    }

    override fun popDelete() = offer(Touch.DELETE)
    override fun popMoveTo() = offer(Touch.MOVE_TO)
    override fun popRename() = offer(Touch.RENAME)
    override fun popSelectAll() = offer(Touch.SELECT_ALL)
    override fun popSetCate() = offer(Touch.ADD_CATE)
    override fun popIntoBox() = offer(Touch.IntoBOX)
    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.text) {
            context.getString(R.string.photo) -> rightMailer = 0
            context.getString(R.string.video) -> rightMailer = 1
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

    override fun onTabReselected(tab: TabLayout.Tab?) = Unit


}