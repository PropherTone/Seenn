package com.protone.seen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.protone.api.context.navigationBarHeight
import com.protone.api.context.statuesBarHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

abstract class Seen<C>(val context: Context) :
    CoroutineScope by CoroutineScope(Dispatchers.Unconfined) {

    abstract val viewRoot: View

    val viewEvent = Channel<C>(Channel.UNLIMITED)

    abstract fun getToolBar(): View?

    abstract fun offer(event: C)

    protected fun initToolBar() {
        getToolBar()?.apply {
            setPadding(
                paddingLeft,
                paddingTop + context.statuesBarHeight,
                paddingRight,
                paddingBottom
            )
        }
        setNavigation()
    }

    protected fun setSettleToolBar() {
        getToolBar()?.apply {
            layoutParams = ConstraintLayout.LayoutParams(width, context.statuesBarHeight)
        }
        setNavigation()
    }

    private fun setNavigation() {
        viewRoot.apply {
            setPadding(
                paddingLeft,
                paddingTop,
                paddingRight,
                paddingBottom + context.navigationBarHeight
            )
        }
    }

}

