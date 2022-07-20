package com.protone.api.context

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.newLayoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

val Context.root: ViewGroup?
    get() {
        return when (this) {
            is Activity -> {
                findViewById(android.R.id.content)
            }
            else -> null
        }
    }

val Context.statuesBarHeight: Int
    get() {
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        var height = 0
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId)
        }
        return height
    }

val Context.navigationBarHeight: Int
    get() {
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        var height = 0
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId)
        }
        return height
    }

var isKeyBroadShow = false

inline fun Activity.setSoftInputStatuesListener(crossinline onSoftInput: (Int, Boolean) -> Unit={ _, _->}) {
    isKeyBroadShow = false
    window.decorView.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
        val rect = Rect()
        v.getWindowVisibleDisplayFrame(rect)
        val i = v.height - rect.bottom - navigationBarHeight
        if (i > 0 && !isKeyBroadShow) {
            isKeyBroadShow = true
            onSoftInput.invoke(i, i > 0)
        } else if (i <= 0 && isKeyBroadShow) {
            isKeyBroadShow = false
            onSoftInput.invoke(i, false)
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
fun Context.linkInput(target: View, input: View) {
    target.setOnTouchListener { _, _ ->
        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputManager.isActive) {
            inputManager.hideSoftInputFromWindow(input.windowToken, 0)
            isKeyBroadShow = false
        }
        false
    }
}

fun View.paddingTop(padding: Int) {
    setPadding(
        paddingLeft,
        paddingTop + padding,
        paddingRight,
        paddingBottom
    )
}

fun View.paddingBottom(padding: Int) {
    setPadding(
        paddingLeft,
        paddingTop,
        paddingRight,
        paddingBottom + padding
    )
}

fun View.marginTop(margin: Int) {
    if (this !is ViewGroup) return
    val marginLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
    marginLayoutParams.topMargin = margin
    layoutParams = marginLayoutParams
}

fun View.marginBottom(margin: Int) {
    if (this !is ViewGroup) return
    val marginLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
    marginLayoutParams.bottomMargin = margin
    layoutParams = marginLayoutParams
}

inline fun Context.onUiThread(crossinline function: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        function.invoke()
        return
    }
    when (this) {
        is Activity -> runOnUiThread { function.invoke() }
        else -> CoroutineScope(Dispatchers.Main).launch { function.invoke() }
    }
}

inline fun onBackground(crossinline function: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        Thread {
            function.invoke()
        }.start()
    } else function.invoke()
}
