package com.protone.ui.customView.richText

import android.net.Uri
import android.os.Parcel
import android.text.Spannable
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.widget.EditText
import com.protone.api.baseType.deleteFile
import com.protone.api.baseType.imageSaveToDisk
import com.protone.api.baseType.toBitmap
import com.protone.api.entity.*
import com.protone.api.json.jsonToList
import com.protone.api.json.listToJson
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.onResult
import com.protone.ui.customView.richText.note.IRichNoteImageLoader
import com.protone.ui.customView.richText.note.spans.ISpanForEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class RichNoteHandler(private val editor: ISpanForEditor) {

    companion object {
        const val TEXT = 0X01
        const val PHOTO = 0X02
        const val MUSIC = 0x03
        const val VIDEO = 0x04
    }

    var curPosition = 0
        private set

    var inIndex = false
        private set

    var imageLoader: IRichNoteImageLoader? = null

    private val deletedMedias by lazy { mutableListOf<String>() }

    fun addDeleteMedias(media: String) = deletedMedias.add(media)

    fun updatePosition(index: Int) {
        if (!inIndex) curPosition = index
    }

    fun getCurrentEditText() = getEditText(curPosition)

    fun getCurrentChild() = editor.getChild(curPosition)

    /**
     * Get current [RichStates]
     *
     * @return [RichStates] or null when caught [IndexOutOfBoundsException]
     */
    fun getCurRichStates(): RichStates? {
        return try {
            val tag = getCurrentChild()?.tag ?: return null
            when (tag) {
                is RichStates -> tag
                else -> null
            }
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    /**
     * Basic method for set span and update [RichNoteStates]
     *
     * @param targetSpan Target span
     */
    fun setEditTextSpan(
        targetSpan: SpanStates.Spans,
        iColor: Any? = null,
        absoluteSize: Int? = null,
        relativeSize: Float? = null,
        scaleX: Float? = null,
        style: Int? = null,
        url: String? = null
    ) {
        getCurrentEditText()?.also {
            val start = it.selectionStart
            val end = it.selectionEnd
            val span = SpanStates(
                start,
                end,
                targetSpan,
                iColor,
                absoluteSize,
                relativeSize,
                scaleX,
                style,
                url
            )
            var isCancellableSpan = false
            val spans = span.getTargetSpan()?.let { targetSpan ->
                isCancellableSpan = targetSpan is AbsoluteSizeSpan
                val getSpans = it.text.getSpans(
                    it.selectionStart,
                    it.selectionEnd,
                    targetSpan.javaClass
                )
                if (targetSpan is StyleSpan) {
                    getSpans.filter { charStyle -> (charStyle as StyleSpan).style == style }
                        .toTypedArray()
                } else getSpans
            }
            val styleList = mutableListOf<SpanStyle>()
            if (spans?.isNotEmpty() == true) {
                fun addStyle(start: Int, end: Int) {
                    span.getTargetSpan()?.let { style ->
                        styleList.add(SpanStyle(style, start, end))
                    }
                }
                spans.forEach { eachSpan ->
                    val spanStart = it.text.getSpanStart(eachSpan)
                    val spanEnd = it.text.getSpanEnd(eachSpan)
                    when {
                        end in spanStart..spanEnd -> {
                            if (start > spanStart && spanStart != start) {
                                addStyle(spanStart, start)
                            }
                            if (end != spanEnd) {
                                addStyle(end, spanEnd)
                            }
                        }
                        start in spanStart..spanEnd -> {
                            if (end < spanEnd && end != spanEnd) {
                                addStyle(end, spanEnd)
                            }
                            if (spanStart != start) {
                                addStyle(spanStart, start)
                            }
                        }
                    }
                    it.text.removeSpan(eachSpan)
                }
            }
            if (spans?.isNotEmpty() == false || isCancellableSpan) {
                span.getTargetSpan()?.let { style ->
                    styleList.add(SpanStyle(style, start, end))
                }
            }
            styleList.forEach { spanStyle ->
                it.text.setSpan(
                    spanStyle.span,
                    spanStyle.start,
                    spanStyle.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            getCurRichStates().let { rs ->
                if (rs is RichNoteStates?) rs?.apply {
                    this.text = it.text
                }
                getCurrentEditText()?.tag = rs
            }
        }
    }

    /**
     * Generate rich text
     *
     * @param list List of [RichStates] use for generating
     */
    suspend fun setRichList(list: List<RichStates>) {
        editor.apply {
            inIndex = true
            list.forEach {
                when (it) {
                    is RichNoteStates -> insertText(it)
                    is RichVideoStates -> insertVideo(it)
                    is RichPhotoStates -> insertImage(it, it.uri.toBitmap())
                    is RichMusicStates -> insertMusic(it)
                }
                curPosition++
            }
            inIndex = false
        }
    }

    suspend fun setRichList(richCode: Int, text: String) {
        var code = richCode
        val statesStrings = text.jsonToList(String::class.java)
        var listSize = statesStrings.size - 1
        val richList = arrayListOf<RichStates>()
        while (code > 0) {
            richList.add(
                when (code % 10) {
                    PHOTO -> {
                        statesStrings[listSize--].toEntity(RichPhotoStates::class.java)
                    }
                    MUSIC -> {
                        statesStrings[listSize--].toEntity(RichMusicStates::class.java)
                    }
                    VIDEO -> {
                        statesStrings[listSize--].toEntity(RichVideoStates::class.java)
                    }
                    else -> {
                        val richNoteSer =
                            statesStrings[listSize--].toEntity(RichNoteSer::class.java)
                        RichNoteStates(richNoteSer.text, mutableListOf())
                    }
                }
            )
            code /= 10
        }
        setRichList(richList.reversed())
    }

    /**
     * Indexing rich note for database to store
     *
     * @return [Pair]
     * <[Int],[String]>
     *
     * [Int] : A sort of number that mark the [RichStates] from left to right
     *
     * [String] : Json of [RichStates]>
     */
    suspend fun indexRichNote(
        title: String,
        childCount: Int,
        onSaveResult: suspend (ArrayList<Uri>) -> Boolean
    ): Pair<Int, String> {
        editor.apply {
            val richArray = arrayOfNulls<String>(childCount)
            val richSer = arrayListOf<String>()
            val reList = arrayListOf<Uri>()
            val taskChannel = Channel<Pair<String, Int>>(Channel.UNLIMITED)
            var richStates = 0
            var count = 0
            return onResult(Dispatchers.Default) {
                async(Dispatchers.IO) {
                    deletedMedias.forEach {
                        it.deleteFile()
                    }
                }.start()
                async(Dispatchers.Default) {
                    while (isActive) {
                        taskChannel.receiveAsFlow().buffer().collect {
                            if (it.second < childCount) {
                                richArray[it.second] = it.first
                                count++
                            }
                            if (count >= childCount) {
                                taskChannel.close()
                            }
                        }
                        if (withContext(Dispatchers.Main) {
                                onSaveResult.invoke(reList)
                            }) {
                            richArray.onEach {
                                it?.let { state ->
                                    richSer.add(state)
                                }
                            }
                            it.resumeWith(
                                Result.success(
                                    Pair(
                                        richStates,
                                        richSer.listToJson(String::class.java)
                                    )
                                )
                            )
                        }
                        break
                    }
                }.start()
                for (i in 0 until childCount) {
                    val view = getChild(i)
                    if (view != null) when (val tag = view.tag) {
                        is RichNoteStates -> {
                            richStates = richStates * 10 + TEXT
                            taskChannel.offer(
                                Pair(
                                    RichNoteSer(
                                        getEditText(i)?.text?.let {
                                            val parcel = Parcel.obtain()
                                            try {
                                                TextUtils.writeToParcel(it, parcel, 0)
                                                val marshall = parcel.marshall()
                                                String(Base64.encode(marshall, Base64.DEFAULT))
                                            } catch (e: Exception) {
                                                ""
                                            } finally {
                                                parcel.recycle()
                                            }
                                        } ?: "",
                                        ""
                                    ).toJson(), i
                                )
                            )
                        }
                        is RichVideoStates -> {
                            richStates = richStates * 10 + VIDEO
                            taskChannel.offer(Pair(tag.toJson(), i))
                        }
                        is RichPhotoStates -> {
                            richStates = richStates * 10 + PHOTO
                            if (tag.path != null) {
                                taskChannel.offer(Pair(tag.toJson(), i))
                            } else async(Dispatchers.IO) {
                                tag.uri.imageSaveToDisk(
                                    tag.name + "_${title}",
                                    dir = title,
                                    w = view.measuredWidth,
                                    h = view.measuredHeight
                                )?.let {
                                    if (tag.path == null) {
                                        tag.path = it
                                    } else {
                                        reList.add(tag.uri)
                                    }
                                }
                                taskChannel.offer(Pair(tag.toJson(), i))
                            }.start()
                        }
                        is RichMusicStates -> {
                            richStates = richStates * 10 + MUSIC
                            taskChannel.offer(Pair(tag.toJson(), i))
                        }
                    }
                }

            }

        }
    }

    /**
     * Get edittext
     *
     * @param position target
     * @return [EditText] or null when target is not [EditText] or caught [IndexOutOfBoundsException]
     */
    private fun getEditText(position: Int): EditText? {
        return try {
            editor.getChild(position).let {
                if (it is EditText) it else null
            }
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }
}