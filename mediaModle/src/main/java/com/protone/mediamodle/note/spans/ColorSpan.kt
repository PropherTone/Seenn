package com.protone.mediamodle.note.spans

import android.content.Context
import android.graphics.Color
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes

/**
 * 快速渲染文字颜色
 * @param color 字体颜色
 * @see ForegroundColorSpan
 */
class ColorSpan(color: Int) : ForegroundColorSpan(color) {
    constructor(color: String) : this(Color.parseColor(color))

    /**
     * 快速创建[ForegroundColorSpan]
     * @param colorRes 字体颜色
     */
    constructor(
        context: Context,
        @ColorRes colorRes: Int
    ) : this(context.resources.getColor(colorRes, context.resources.newTheme()))
}

class BackColorSpan(color: Int) : BackgroundColorSpan(color) {
    constructor(color: String) : this(Color.parseColor(color))

    /**
     * 快速创建[BackgroundColorSpan]
     * @param colorRes 字体颜色
     */
    constructor(
        context: Context,
        @ColorRes colorRes: Int
    ) : this(context.resources.getColor(colorRes, context.resources.newTheme()))
}