package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.MyFragmentStateAdapter
import com.protone.seen.databinding.GalleyLayoutBinding
import com.protone.seen.databinding.GalleyOptionPopBinding
import com.protone.seen.popWindows.GalleyOptionPop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext


class GalleySeen(context: Context) : Seen<GalleySeen.Touch>(context),
    TabLayout.OnTabSelectedListener, View.OnClickListener {

    enum class Touch {
        Finish,
        ShowOptionMenu,
        DELETE,
        RENAME,
        MOVE_TO,
        SELECT_ALL,
        ADD_CATE,
        IntoBOX
    }

    private val binding = GalleyLayoutBinding.inflate(context.layoutInflater, context.root, false)

    private val popLayout = GalleyOptionPopBinding.inflate(context.layoutInflater, context.root, false)

    private val pop = GalleyOptionPop(context, popLayout.root)

    private lateinit var rightChannel: Channel<GalleyFragment.Event>

    val chooseData: MutableLiveData<MutableList<GalleyMedia>> =
        MutableLiveData<MutableList<GalleyMedia>>().apply {
            observe(context as LifecycleOwner) {
                it?.let {
                    binding.galleyActionMenu.isVisible = it.size > 0
                }
            }
        }

    override val viewRoot: View
        get() = binding.root

    init {
        binding.self = this
        binding.galleyActionMenu.setOnClickListener(this@GalleySeen)
        popLayout.apply {
            galleyDelete.setOnClickListener(this@GalleySeen)
            galleyMoveTo.setOnClickListener(this@GalleySeen)
            galleyRename.setOnClickListener(this@GalleySeen)
            galleySelectAll.setOnClickListener(this@GalleySeen)
            galleySetCate.setOnClickListener(this@GalleySeen)
            imageView6.setOnClickListener(this@GalleySeen)
        }

    }

    fun offer(msg: Touch) {
        viewEvent.offer(msg)
    }

    fun setOptionButton(visible: Boolean) {
        binding.galleyActionMenu.isVisible = visible
    }

    fun offer(msg: GalleyFragment.Event) {
        rightChannel.offer(msg)
    }

    fun showPop() {
        pop.showPop(binding.galleyActionMenu)
//        GalleyOptionPop(context, binding.galleyActionMenu)
    }

    suspend fun initPager(
        galleyMediaList: MutableMap<String, MutableList<GalleyMedia>>,
        videoMediaList: MutableMap<String, MutableList<GalleyMedia>>
    ) = withContext(Dispatchers.Main) {
        binding.galleyPager.let {
            it.adapter = MyFragmentStateAdapter(
                context as FragmentActivity,
                arrayListOf(
                    GalleyFragment(context, galleyMediaList, chooseData),
                    GalleyFragment(
                        context,
                        videoMediaList,
                        chooseData,
                        isVideo = true
                    )
                )
            )
            TabLayoutMediator(
                binding.galleyTab.apply {
                    addOnTabSelectedListener(this@GalleySeen)
                },
                it
            ) { tab, position ->
                tab.setText(
                    arrayOf(
                        R.string.photo,
                        R.string.video
                    )[position]
                )
            }.attach()
        }
    }

    override fun onClick(p0: View?) {
        popLayout.apply {
            when (p0) {
                galleyDelete -> offer(Touch.DELETE)
                galleyMoveTo -> offer(Touch.MOVE_TO)
                galleyRename -> offer(Touch.RENAME)
                galleySelectAll -> offer(Touch.SELECT_ALL)
                galleySetCate -> offer(Touch.ADD_CATE)
                imageView6 -> offer(Touch.IntoBOX)
                binding.galleyActionMenu -> offer(Touch.ShowOptionMenu)
            }
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {}
    override fun onTabUnselected(tab: TabLayout.Tab?) {}
    override fun onTabReselected(tab: TabLayout.Tab?) {}


}