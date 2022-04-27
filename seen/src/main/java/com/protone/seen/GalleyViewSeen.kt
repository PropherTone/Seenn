package com.protone.seen

import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.protone.api.TAG
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.adapter.GalleyViewPager2Adapter
import com.protone.seen.databinding.GalleyViewLayouyBinding

class GalleyViewSeen(context: Context) : Seen<GalleyViewSeen.GalleyVEvent>(context) {

    enum class GalleyVEvent {
        Finish,
        ShowAction,
        SetNotes
    }

    private val binding =
        GalleyViewLayouyBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        initToolBar()
        binding.self = this
        binding.galleyVCover.setOnClickListener {
            Log.d(TAG, "clk: ")
            binding.galleyVCover.isVisible = !binding.galleyVCover.isVisible
        }
        binding.galleyVView.setOnClickListener {
            Log.d(TAG, "clk: 1")
            binding.galleyVCover.isVisible = !binding.galleyVCover.isVisible
        }
        binding.root.setOnClickListener {
            Log.d(TAG, "clk: 2")
            binding.galleyVCover.isVisible = !binding.galleyVCover.isVisible
        }
    }

    fun initViewPager(position: Int, data: MutableList<GalleyMedia>, onChange: (Int) -> Unit) {
        binding.galleyVView.apply {
            adapter = GalleyViewPager2Adapter(context, data)
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

    override fun offer(event: GalleyVEvent) {
        viewEvent.offer(event)
    }

}