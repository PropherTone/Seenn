package com.protone.seen

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.protone.api.context.layoutInflater
import com.protone.api.context.navigationBarHeight
import com.protone.api.context.root
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.adapter.PictureBoxAdapter
import com.protone.seen.databinding.PictureBoxLayoutBinding

class PictureBoxSeen(context: Context) : Seen<PictureBoxSeen.PictureBox>(context) {

    enum class PictureBox {
        SelectPicList
    }

    val binding = PictureBoxLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        binding.self = this
    }

    override fun offer(event: PictureBox) {
        viewEvent.trySend(event)
    }

    fun initPictureBox(picUri: MutableList<GalleyMedia>) {
        binding.picView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = PictureBoxAdapter(context, picUri)
        }
    }
}