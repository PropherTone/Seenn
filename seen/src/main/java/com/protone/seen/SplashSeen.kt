package com.protone.seen

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import com.protone.api.Config
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.SplashLayoutBinding

class SplashSeen(context: Context) : Seen<SplashSeen.Event>(context),
    ViewTreeObserver.OnGlobalLayoutListener {

    enum class Event

    private val binding = SplashLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View
        get() = binding.root

    init {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        binding.root.let {
            Config.apply {
                screenHeight = it.measuredHeight
                screenWidth = it.measuredWidth
            }
            it.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    override fun offer(event: Event) {

    }

    override fun getToolBar(): View = View(context)
}