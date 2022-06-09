package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.media.FragMailer
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
        ShowPop,
        Init
    }

    companion object {
        @JvmStatic
        val CHOOSE_PHOTO = "PHOTO"

        @JvmStatic
        val CHOOSE_VIDEO = "VIDEO"
    }

    private val binding = GalleyLayoutBinding.inflate(context.layoutInflater, context.root, false)

    private val mailers = arrayOfNulls<FragMailer>(2)
    fun setMailer(frag1: FragMailer? = null, frag2: FragMailer? = null) {
        if (frag1 != null) {
            mailers[0] = frag1
        } else if (frag2 != null) {
            mailers[1] = frag2
        }
    }

    var rightMailer = 0

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
        fragments: ArrayList<Fragment>,
        chooseType: String = "",
    ) = withContext(Dispatchers.Main) {
        initViewMode(chooseType.isNotEmpty())
        binding.galleyPager.let {
            it.adapter = MyFragmentStateAdapter(
                context as FragmentActivity,
                when (chooseType) {
                    CHOOSE_PHOTO -> arrayListOf(fragments[0])
                    CHOOSE_VIDEO -> arrayListOf(fragments[1])
                    else -> fragments
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

    fun showPop(isSelect: Boolean) {
        showPop(binding.galleyActionMenu, !isSelect)
    }

    fun onAction() {
        mailers[rightMailer]?.onActionBtn()
    }

    fun addBucket(name: String, list: MutableList<GalleyMedia>) {
        mailers[rightMailer]?.addBucket(name, list)
    }

    fun deleteMedia(media: GalleyMedia) {
        mailers[rightMailer]?.deleteMedia(media)
    }

    fun getChooseGalley(): MutableList<GalleyMedia>? {
        return mailers[rightMailer]?.getChooseGalley()
    }

    fun selectAll() {
        mailers[rightMailer]?.selectAll()
    }

    fun showActionBtn() {
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