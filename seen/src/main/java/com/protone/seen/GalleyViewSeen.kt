package com.protone.seen

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.adapter.GalleyViewPager2Adapter
import com.protone.seen.databinding.GalleyViewLayouyBinding

class GalleyViewSeen(context: Context) : Seen<GalleyViewSeen.GalleyVEvent>(context) {

    enum class GalleyVEvent {
        Finish,
        ShowAction
    }

    private val binding =
        GalleyViewLayouyBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.galleyVTitle

    init {
        initToolBar()
    }

    fun initViewPager(position: Int, data: MutableList<GalleyMedia>, onChange: () -> Unit) {
        binding.galleyVView.apply {
            adapter = GalleyViewPager2Adapter(context, data)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    onChange.invoke()
                }
            })
            setCurrentItem(position, false)
        }
    }

    fun initList(startNote: (String) -> Unit) {
        binding.galleyVLinks.apply {
            adapter = CheckListAdapter(context).also {
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
        notes: MutableList<String>
    ) = binding.run {
        galleyVTitle.text = title
        galleyVTime.text = time
        galleyVSize.text = size
        galleyVCato.text = cato
        galleyVType.text = type

    }

    override fun offer(event: GalleyVEvent) {
        viewEvent.offer(event)
    }

}