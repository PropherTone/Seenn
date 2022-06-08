package com.protone.api

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class SearchModel(editText: EditText, private val query: () -> Unit) : TextWatcher {

    private val timerHandler = Handler(Looper.getMainLooper()) {
        if (it.what == 0) {
            query.invoke()
        }
        false
    }

    private var delayed = 500L

    private var input = ""

    init {
        editText.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun afterTextChanged(s: Editable?) {
        input = s.toString()
        timerHandler.sendEmptyMessageDelayed(0, delayed)
    }

    fun getInput() = input
}