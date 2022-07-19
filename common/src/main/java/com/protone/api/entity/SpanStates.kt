package com.protone.api.entity

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.*
import android.view.View

data class SpanStates(
    var start: Int,
    var end: Int,
    val targetSpan: Spans,
    val iColor: Any? = Color.BLACK,
    val absoluteSize: Int? = 1,
    val relativeSize: Float? = 1f,
    val scaleX: Float? = 1f,
    val style: Int? = 0,
    val url: String? = null
) {

    enum class Spans {
        BackgroundColorSpan,// 背景色
        ClickableSpan,// 文本可点击，有点击事件
        ForegroundColorSpan,//文本颜色（前景色）
        StrikeThroughSpan,//删除线（中划线）
        UnderlineSpan,//下划线
        AbsoluteSizeSpan,// 绝对大小（文本字体）
        RelativeSizeSpan,// 相对大小（文本字体）
        ScaleXSpan,// 基于x轴缩放
        StyleSpan,//字体样式：粗体、斜体等
        SubscriptSpan,//下标（数学公式会用到）
        SuperscriptSpan,//上标（数学公式会用到）
        TypefaceSpan,//文本字体
        URLSpan// 文本超链接
    }

    private fun getColorSpan(back: Boolean): CharacterStyle? =
        iColor?.let {
            when (it) {
                is Int -> if (back) BackColorSpan(it) else ColorSpan(it)
                is String -> if (back) BackColorSpan(it) else ColorSpan(it)
                else -> if (back) BackColorSpan(Color.BLACK) else ColorSpan(Color.BLACK)
            }
        }

    fun getTargetSpan(): CharacterStyle? = when (targetSpan) {
        Spans.BackgroundColorSpan -> {
            getColorSpan(true)
        }
        Spans.ClickableSpan -> {
            object : ClickableSpan() {
                override fun onClick(p0: View) {

                }
            }
        }
        Spans.ForegroundColorSpan -> {
            getColorSpan(false)
        }
        Spans.StrikeThroughSpan -> {
            StrikethroughSpan()
        }
        Spans.UnderlineSpan -> {
            UnderlineSpan()
        }
        Spans.AbsoluteSizeSpan -> {
            absoluteSize?.let { AbsoluteSizeSpan(it) }
        }
        Spans.RelativeSizeSpan -> {
            relativeSize?.let { RelativeSizeSpan(it) }
        }
        Spans.ScaleXSpan -> {
            scaleX?.let { ScaleXSpan(it) }
        }
        Spans.StyleSpan -> {
            style?.let { StyleSpan(it) }
        }
        Spans.SubscriptSpan -> {
            SubscriptSpan()
        }
        Spans.SuperscriptSpan -> {
            SuperscriptSpan()
        }
        Spans.TypefaceSpan -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            TypefaceSpan(Typeface.MONOSPACE)
        } else {
            null
        }
        Spans.URLSpan -> {
            URLSpan(url)
        }
    }
}
