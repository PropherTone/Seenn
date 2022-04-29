package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.MyFragmentStateAdapter
import com.protone.seen.databinding.GalleyLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class GalleySeen(context: Context) : PopupCoverSeen<GalleySeen.Touch>(context){

    enum class Touch {
        Finish,
        DELETE,
        RENAME,
        MOVE_TO,
        SELECT_ALL,
        ADD_CATE,
        IntoBOX,
        ConfirmChoose
    }

    companion object {
        @JvmStatic
        val CHOOSE_PHOTO = "PHOTO"

        @JvmStatic
        val CHOOSE_VIDEO = "VIDEO"
    }

    private val binding = GalleyLayoutBinding.inflate(context.layoutInflater, context.root, false)

    private lateinit var rightChannel: Channel<GalleyFragment.Event>

    val chooseData: MutableLiveData<MutableList<GalleyMedia>> =
        MutableLiveData<MutableList<GalleyMedia>>().apply {
            observe(context as LifecycleOwner) {
                it?.let {
                    setOptionButton(it.size > 0)
                }
            }
        }

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.view

    init {
        setSettleToolBar()
        binding.self = this
    }

    private fun initViewMode(chooseMode: Boolean) {
        if (chooseMode) {
            binding.galleyChooseConfirm.isGone = !chooseMode
            binding.galleyChooseConfirm.setOnClickListener { offer(Touch.ConfirmChoose) }
            binding.galleyActionMenu.isGone = chooseMode
        }
    }

    override fun offer(event: Touch) {
        viewEvent.offer(event)
    }

    fun setOptionButton(visible: Boolean) {
        if (!binding.galleyActionMenu.isGone) binding.galleyActionMenu.isVisible = visible
    }

    fun offer(msg: GalleyFragment.Event) {
        rightChannel.offer(msg)
    }

    suspend fun initPager(
        galleyMediaList: MutableMap<String, MutableList<GalleyMedia>>,
        videoMediaList: MutableMap<String, MutableList<GalleyMedia>>,
        chooseType: String = "",
        openView: (GalleyMedia, Boolean) -> Unit
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
                )
            }
            val videoFragment by lazy {
                GalleyFragment(
                    context as FragmentActivity,
                    videoMediaList,
                    chooseData,
                    multiChoose = chooseType.isNotEmpty(),
                    isVideo = true,
                    openView = openView
                )
            }
            it.adapter = MyFragmentStateAdapter(
                context as FragmentActivity,
                when (chooseType) {
                    CHOOSE_PHOTO -> arrayListOf(photoFragment)
                    CHOOSE_VIDEO -> arrayListOf(videoFragment)
                    else -> arrayListOf(photoFragment, videoFragment)
                }
            )
            TabLayoutMediator(binding.galleyTab, it) { tab, position ->
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

    fun showPop(){
        showPop(binding.galleyActionMenu)
    }

    override fun popDelete() = offer(Touch.DELETE)
    override fun popMoveTo() = offer(Touch.MOVE_TO)
    override fun popRename() = offer(Touch.RENAME)
    override fun popSelectAll() = offer(Touch.SELECT_ALL)
    override fun popSetCate() = offer(Touch.ADD_CATE)
    override fun popIntoBox() = offer(Touch.IntoBOX)


}