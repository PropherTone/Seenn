package com.protone.seen

import android.content.Context
import android.view.View
import androidx.core.view.isGone
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.GalleyOptionPopBinding
import com.protone.seen.popWindows.GalleyOptionPop

abstract class PopupCoverSeen<C>(context: Context) : Seen<C>(context), View.OnClickListener {

    val popLayout =
        GalleyOptionPopBinding.inflate(context.layoutInflater, context.root, false)

    private val pop = GalleyOptionPop(context, popLayout.root)

    init {
        popLayout.apply {
            galleyDelete.setOnClickListener(this@PopupCoverSeen)
            galleyMoveTo.setOnClickListener(this@PopupCoverSeen)
            galleyRename.setOnClickListener(this@PopupCoverSeen)
            galleySelectAll.setOnClickListener(this@PopupCoverSeen)
            galleySetCate.setOnClickListener(this@PopupCoverSeen)
            galleyIntoBox.setOnClickListener(this@PopupCoverSeen)
        }
    }

    fun showPop(anchor: View, isSelect: Boolean) {
        popLayout.apply {
            galleyDelete.isGone = isSelect
            galleyMoveTo.isGone = isSelect
            galleyRename.isGone = isSelect
            galleySetCate.isGone = isSelect
        }
        pop.showPop(anchor)
    }

    override fun onClick(v: View?) {
        popLayout.apply {
            when (v) {
                galleyDelete -> popDelete()
                galleyMoveTo -> popMoveTo()
                galleyRename -> popRename()
                galleySelectAll -> popSelectAll()
                galleySetCate -> popSetCate()
                galleyIntoBox -> popIntoBox()
            }
        }
        pop.dismiss()
    }

    abstract fun popDelete()
    abstract fun popMoveTo()
    abstract fun popRename()
    abstract fun popSelectAll()
    abstract fun popSetCate()
    abstract fun popIntoBox()
}