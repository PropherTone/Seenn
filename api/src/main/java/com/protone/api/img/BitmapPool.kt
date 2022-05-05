package com.protone.api.img

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class BitmapPool : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val pool = arrayOfNulls<Bitmap>(3)

    private var index = 0

    fun add(bitmap: Bitmap) {
        when {
            index == 2 -> {
                recycle(0)
            }
            index > 2 -> {
                index = 0;
                recycle(1)
            }
            index == 1 -> {
                recycle(2)
            }
            index == 0 -> {
                recycle(1)
            }
        }
        pool[index++] = bitmap
    }

    fun get(): Bitmap? = pool[index - 1]

    private fun recycle(index: Int) {
        if (pool[index] != null && pool[index]?.isRecycled == false) {
            pool[index]?.recycle()
            pool[index] = null
        }
    }
}