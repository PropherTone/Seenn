//package com.protone.seen.customView
//
//import android.content.Context
//import android.util.AttributeSet
//import android.view.View
//import android.view.ViewGroup
//import android.widget.LinearLayout
//import android.widget.ScrollView
//import androidx.annotation.AttrRes
//import androidx.annotation.StyleRes
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.MainScope
//import kotlinx.coroutines.launch
//
//abstract class LoadScrollView<VH : LoadScrollView.LoadViewHolder> @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    @AttrRes defStyleAttr: Int = 0,
//    @StyleRes defStyleRes: Int = 0
//) : ScrollView(context, attrs, defStyleAttr, defStyleRes), CoroutineScope by MainScope() {
//
//    lateinit var container: LinearLayout
//
//    private val holders = mutableListOf<VH>()
//
//    init {
//        launch {
//            initView()
//        }
//    }
//
//    private fun initView() {
//        addView(LinearLayout(context).apply {
//            layoutParams = LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )
//            orientation = LinearLayout.VERTICAL
//            container = this
//        })
//        for (i in 0 until getDataCount()) {
//            container.
//        }
//    }
//
//    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
//        super.onScrollChanged(l, t, oldl, oldt)
//        if (t > oldt) {
//            //Scroll down
//            doOnScrollDown()
//        } else if (t < oldt) {
//            //Scroll up
//            doOnScrollUp()
//        }
//    }
//
//    private fun createLoadViewHolder(parent: ViewGroup): VH {
//        return onCreateLoadViewHolder(parent)
//    }
//
//    abstract fun onCreateLoadViewHolder(parent: ViewGroup): VH
//
//    private fun doOnScrollDown() {
//        val holder = createLoadViewHolder(this)
//        holders.add(holder)
//        container.addView(holder.loadView)
//        onScrollDown(holder, holders.indexOf(holder))
//    }
//
//    private fun doOnScrollUp() {
////        onScrollUp()
//    }
//
//    abstract fun onScrollUp(holder: VH, position: Int)
//    abstract fun onScrollDown(holder: VH, position: Int)
//    abstract fun getDataCount(): Int
//    abstract class LoadViewHolder(val loadView: View)
//
//    abstract suspend fun onPreLoad(count: Int): IntArray
//
//}