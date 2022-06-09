package com.protone.seen

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.protone.api.context.hasNavigationBar
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

    private var inputManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /**
     * Let view padding top to full the toolbar
     */
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

    /**
     * Use a view to full the toolbar
     */
    protected fun setSettleToolBar() {
        getToolBar()?.apply {
            layoutParams = ConstraintLayout.LayoutParams(width, context.statuesBarHeight)
        }
        setNavigation()
    }


    fun setNavigation() {
        if (context.hasNavigationBar) viewRoot.apply {
            setPadding(
                paddingLeft,
                paddingTop,
                paddingRight,
                paddingBottom + context.navigationBarHeight
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun linkInput(target: View, arch: View) {
        target.setOnTouchListener { _, _ ->
            if (inputManager.isActive) {
                inputManager.hideSoftInputFromWindow(arch.windowToken, 0)
            }
            false
        }

    }

}

