package com.protone.api.spans

import android.graphics.Color
import android.os.Build
import android.text.Layout
import android.text.style.*
import android.view.View
import java.util.*

data class SpanStates(var start: Int, var end: Int, val targetSpan: Spans) {

    sealed class Spans {
        data class BackgroundColor(val color: Any) : Spans() // 背景色
        data class Clickable(val objects: Objects) : Spans() // 文本可点击，有点击事件
        data class ForegroundColor(val color: Any) : Spans() //文本颜色（前景色）
        object StrikeThrough : Spans() //删除线（中划线）
        object Underline : Spans() //下划线
        data class AbsoluteSize(val absoluteSize: Int) : Spans() // 绝对大小（文本字体）
        data class RelativeSize(val relativeSize: Float) : Spans() // 相对大小（文本字体）
        data class ScaleX(val scaleX: Float) : Spans() // 基于x轴缩放
        data class Style(val style: Int) : Spans() //字体样式：粗体、斜体等
        object Subscript : Spans() //下标（数学公式会用到）
        object Superscript : Spans() //上标（数学公式会用到）
        data class Typeface(val typeface: android.graphics.Typeface) : Spans() //文本字体
        data class URL(val url: String) : Spans() // 文本超链接

        data class Bullet(
            val gapWidth: Int? = null,
            val color: Any? = null,
            val radius: Int? = null
        ) : Spans() //列表圆点

        data class Quote(
            val color: Any? = null,
            val stripeWidth: Int? = null,
            val gapWidth: Int? = null
        ) : Spans() //文本左侧引用竖线

        data class Paragraph(val alignment: Layout.Alignment) : Spans() //文本对齐
    }

    private fun getColorSpan(back: Boolean, color: Any): Any =
        when (color) {
            is Int -> if (back) BackColorSpan(color) else ColorSpan(color)
            is String -> if (back) BackColorSpan(color) else ColorSpan(color)
            else -> if (back) BackColorSpan(Color.BLACK) else ColorSpan(Color.BLACK)
        }


    private fun checkColor(color: Any?): Int? =
        when (color) {
            is Int -> color
            is String -> Color.parseColor(color)
            else -> null
        }

    fun getTargetSpan(): Any? = when (targetSpan) {
        is Spans.BackgroundColor -> {
            getColorSpan(true, targetSpan.color)
        }
        is Spans.Clickable -> {
            object : ClickableSpan() {
                override fun onClick(p0: View) {

                }
            }
        }
        is Spans.ForegroundColor -> {
            getColorSpan(false, targetSpan.color)
        }
        Spans.StrikeThrough -> {
            StrikethroughSpan()
        }
        Spans.Underline -> {
            UnderlineSpan()
        }
        is Spans.AbsoluteSize -> {
            AbsoluteSizeSpan(targetSpan.absoluteSize)
        }
        is Spans.RelativeSize -> {
            RelativeSizeSpan(targetSpan.relativeSize)
        }
        is Spans.ScaleX -> {
            ScaleXSpan(targetSpan.scaleX)
        }
        is Spans.Style -> {
            StyleSpan(targetSpan.style)
        }
        Spans.Subscript -> {
            SubscriptSpan()
        }
        Spans.Superscript -> {
            SuperscriptSpan()
        }
        is Spans.Typeface -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TypefaceSpan(targetSpan.typeface)
        } else {
            null
        }
        is Spans.URL -> {
            URLSpan(targetSpan.url)
        }
        is Spans.Bullet -> if (targetSpan.gapWidth != null) {
            checkColor(targetSpan.color).let { color ->
                if (color != null) {
                    if (targetSpan.radius != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        return@let BulletSpan(targetSpan.gapWidth, color, targetSpan.radius)
                    }
                    return@let BulletSpan(targetSpan.gapWidth, color)
                }
                return@let BulletSpan(targetSpan.gapWidth)
            }
        } else BulletSpan()
        is Spans.Quote -> checkColor(targetSpan.color).let { color ->
            if (color != null) {
                if (targetSpan.stripeWidth != null
                    && targetSpan.gapWidth != null
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ) {
                    return@let QuoteSpan(color, targetSpan.stripeWidth, targetSpan.gapWidth)
                }
                return@let QuoteSpan(color)
            }
            return@let QuoteSpan()
        }
        is Spans.Paragraph -> {
            AlignmentSpan {
                targetSpan.alignment
            }
        }
    }
}