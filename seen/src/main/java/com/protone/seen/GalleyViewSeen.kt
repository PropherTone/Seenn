package com.protone.seen

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.adapter.GalleyViewPager2Adapter
import com.protone.seen.databinding.GalleyViewLayouyBinding

@SuppressLint("ClickableViewAccessibility")
class GalleyViewSeen(context: Context) : PopupCoverSeen<GalleyViewSeen.GalleyVEvent>(context) {

    enum class GalleyVEvent {
        Finish,
        ShowAction,
        SetNotes,
        Delete,
        Rename,
        MoveTo,
        SelectAll,
        AddCato,
        IntoBox
    }

    private val binding =
        GalleyViewLayouyBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        initToolBar()
        binding.self = this
        popLayout.galleySelectAll.isGone = true
    }

    fun initViewPager(
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
                    if (state == ViewPager2.SCREEN_STATE_OFF) offer(GalleyVEvent.SetNotes)
                }
            })
            setCurrentItem(position, false)
        }
    }

    fun initList(startNote: (String) -> Unit) {
        binding.galleyVLinks.apply {
            adapter = CheckListAdapter(context, check = false).also {
                it.startNote = startNote
            }
            layoutManager = LinearLayoutManager(context)
        }
    }

    fun setMediaInfo(
        title: String,
        time: String,
        size: String,
        cato: String,
        type: String,
    ) = binding.run {
        galleyVTitle.text = title
        galleyVTime.text = String.format(
            context.getString(R.string.time),
            time.ifEmpty { context.getString(R.string.none) })
        galleyVSize.text = String.format(
            context.getString(R.string.size),
            size.ifEmpty { context.getString(R.string.none) })
        galleyVCato.text = String.format(
            context.getString(R.string.cato),
            cato.ifEmpty { context.getString(R.string.none) })
        galleyVType.text = String.format(
            context.getString(R.string.type),
            type.ifEmpty { context.getString(R.string.none) })
    }

    fun setNotes(notes: MutableList<String>) {
        (binding.galleyVLinks.adapter as CheckListAdapter).dataList = notes
    }

    fun showPop() {
        showPop(binding.galleyVAction,true)
    }

    override fun popDelete() = offer(GalleyVEvent.Delete)
    override fun popMoveTo() = offer(GalleyVEvent.MoveTo)
    override fun popRename() = offer(GalleyVEvent.Rename)
    override fun popSelectAll() = offer(GalleyVEvent.SelectAll)
    override fun popSetCate() = offer(GalleyVEvent.AddCato)
    override fun popIntoBox() = offer(GalleyVEvent.IntoBox)
    override fun offer(event: GalleyVEvent) {
        viewEvent.offer(event)
    }

}