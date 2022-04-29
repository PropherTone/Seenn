package com.protone.seen.customView.richText

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.Spanned
import android.text.style.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.protone.api.Config
import com.protone.api.context.layoutInflater
import com.protone.api.context.onBackground
import com.protone.api.context.onUiThread
import com.protone.api.json.jsonToList
import com.protone.api.json.listToJson
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.toDrawable
import com.protone.api.toMediaBitmapByteArray
import com.protone.mediamodle.note.MyTextWatcher
import com.protone.mediamodle.note.entity.*
import com.protone.mediamodle.note.spans.ColorSpan
import com.protone.mediamodle.note.spans.ISpanForEditor
import com.protone.seen.R
import com.protone.seen.customView.DragRect
import com.protone.seen.customView.MyMusicPlayer
import com.protone.seen.databinding.RichMusicLayoutBinding
import com.protone.seen.databinding.RichPhotoLayoutBinding
import com.protone.seen.databinding.RichVideoLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * RichText editor by ProTone 2022/4/15
 *
 * Use [ScrollView] or [NestedScrollView] as editor's parent
 * */
class RichNoteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    var isEditable: Boolean = false
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), ISpanForEditor {

    companion object {
        const val TEXT = 0X01
        const val PHOTO = 0X02
        const val MUSIC = 0x03
        const val VIDEO = 0x04
    }

    private var curPlaying = 0
    private var curPosition = 0

    private var inIndex = false

    init {
        orientation = VERTICAL
        layoutTransition = LayoutTransition()
    }

    /**
     * Generate rich text
     *
     * @param list List of [RichStates] use for generating
     */
    fun setRichList(list: List<RichStates>) {
        removeAllViews()

        inIndex = true
        list.forEach {
            when (it) {
                is RichNoteStates -> insertText(it)
                is RichVideoStates -> insertVideo(it)
                is RichPhotoStates -> insertImage(it)
                is RichMusicStates -> insertMusic(it)
            }
            curPosition++
        }
        inIndex = false
    }

    fun setRichList(richCode: Int, text: String) {
        onBackground {
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
                            val toEntity =
                                statesStrings[listSize--].toEntity(RichNoteSer::class.java)
                            val toEntity1 = toEntity.spans.jsonToList(SpanStates::class.java)
                            RichNoteStates(toEntity.text, toEntity1)
                        }
                    }
                )
                code /= 10
            }
            context.onUiThread {
                setRichList(richList.reversed())
            }
        }
    }

    private fun insertText(note: RichNoteStates) {
//        if (!inIndex) richList.add(note)
        addView(when (isEditable) {
            true ->
                EditText(context).apply {
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundColor(Color.WHITE)
                    setText(note.text)
                    setOnKeyListener { _, keyCode, event ->
                        // Noticed if no text to delete while delete key pressed
                        if (keyCode == KeyEvent.KEYCODE_DEL &&
                            event.action == KeyEvent.ACTION_DOWN &&
                            text.isEmpty()
                        ) {
                            var indexOfChild = indexOfChild(this@apply)
                            if (indexOfChild > 0) {
                                removeViewAt(indexOfChild)
                                //Delete next view if it's not a edittext
                                if (indexOfChild-- > 0 && getChildAt(indexOfChild) !is EditText) {
                                    removeView(getChildAt(indexOfChild--))
                                    //Insert new edittext when there is no place for input
                                    if ((indexOfChild > 0 && indexOfChild in 0 until childCount && getChildAt(
                                            indexOfChild
                                        ) !is EditText) || childCount == 0
                                    ) {
                                        insertText(RichNoteStates("", arrayListOf()))
                                    }
                                }

                            }
                        }
                        false
                    }
                    addTextChangedListener(object : MyTextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            curPosition = this@RichNoteView.indexOfChild(this@apply)
                            getCurRichStates().let {
                                if (it is RichNoteStates) it.apply {
                                    text = s
                                    if (s?.isEmpty() == true) (spanStates as ArrayList?)?.clear()
                                    val iterator = (spanStates as ArrayList?)?.iterator()
                                    while (iterator?.hasNext() == true) {
                                        iterator.next().let { ss ->
                                            if (ss.end > s?.length ?: 0) ss.end = s?.length ?: 0
                                            if (ss.end <= ss.start) iterator.remove()
                                        }
                                    }
                                }
                                tag = it
                            }
                        }
                    })
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            curPosition = this@RichNoteView.indexOfChild(this)
                        }
                    }
                    setOnClickListener { curPosition = this@RichNoteView.indexOfChild(this) }
                    setOnTouchListener { _, _ ->
                        this@RichNoteView.indexOfChild(this)
                        performClick()
                        false
                    }
                    tag = note
                }
            else -> TextView(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = note.text
            }
        }).run {
            this@RichNoteView.requestFocus()
            getEdittext(curPosition)?.also {
                it.requestFocus()
                it.performClick()
            }
        }
    }

    override fun insertVideo(video: RichVideoStates) = insertMedia {
        addView(RichVideoLayoutBinding.inflate(context.layoutInflater, this, false).apply {
            richVideo.setVideoPath(video.uri)
            richLinkContainer.isGone = video.link == null
            if (!isEditable) {
                richLinkContainer.setOnClickListener {
                    video.link?.let { note -> iRichListener?.jumpTo(note) }
                }
                richLink.text = video.link ?: ""
            }
        }.root.also { r -> r.tag = video }, it)
    }

    override fun insertMusic(music: RichMusicStates) = insertMedia {
        addView(RichMusicLayoutBinding.inflate(context.layoutInflater, this, false).apply {
            richMusic.playMusic = {
                iRichListener?.play(music.uri, richMusic.progress)
                curPlaying = this@RichNoteView.indexOfChild(root)
            }
            richMusic.pauseMusic = { iRichListener?.pause() }
            richLinkContainer.isGone = music.link == null
            if (!isEditable) {
                richLinkContainer.setOnClickListener {
                    music.link?.let { note -> iRichListener?.jumpTo(note) }
                }
                richLink.text = music.link ?: ""
            }
            richMusic.icon = music.uri
            richMusic.name = music.name
        }.root.also { r -> r.tag = music }, it)
    }

    fun setMusicProgress(progress: Long) {
        getChildAt(curPlaying)?.let {
            if (it is MyMusicPlayer) {
                it.progress = progress
            }
        }
    }

    fun setMusicDuration(duration: Long) {
        getChildAt(curPosition)?.let {
            if (it is MyMusicPlayer) {
                it.duration = duration
            }
        }
    }

    private fun getBitmapWH(byteArray: ByteArray): IntArray? {
        try {
            val option = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val dba = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, option)
            val height = dba.height
            val width = dba.width
            val index = width / height
            val bmH = Config.screenWidth / index
            dba.recycle()
            return intArrayOf(Config.screenWidth, bmH)
        } catch (e: Exception) {
        }
        return null
    }

    override fun insertImage(photo: RichPhotoStates) = insertMedia {
        addView(RichPhotoLayoutBinding.inflate(context.layoutInflater, this, false).apply {
            val ba = photo.uri.toMediaBitmapByteArray()
            if (ba == null) {
                Glide.with(context).asDrawable()
                    .load(R.drawable.ic_baseline_error_outline_24_black)
                    .into(this.richPhotoIv)
            } else {
                val bitmapWH = getBitmapWH(ba)
                Glide.with(context).load(photo.uri).let { glide ->
                    if (bitmapWH != null) glide.override(bitmapWH[0], bitmapWH[1]) else glide
                }.into(this.richPhotoIv)
                richPhotoTitle.text = photo.name
                richPhotoDetail.text = photo.date
            }
//            drag.isGone = !isEditable
            if (isEditable) {
                drag.rectSizeListener = object : DragRect.RectSize {
                    override fun onChange(x: Int, y: Int) {
                        root.layoutParams = LayoutParams(x, y)
                    }
                }
            }
            richPhotoFull.setOnClickListener { iRichListener?.openImage(photo.uri, photo.name) }
            richPhotoTvContainer.setOnClickListener {
                richPhotoTitle.apply {
                    isVisible = !isVisible
                    if (!drag.isGone) drag.isVisible = isVisible
                    richPhotoDetailContainer.isVisible = isVisible
                }
            }
        }.root.also { v -> v.tag = photo }, it)
    }

    override fun setBold() {
        setEditTextSpan(StyleSpan(Typeface.BOLD), SpanStates.Spans.StyleSpan, style = Typeface.BOLD)
    }

    override fun setItalic() {
        setEditTextSpan(
            StyleSpan(Typeface.ITALIC),
            SpanStates.Spans.StyleSpan,
            style = Typeface.ITALIC
        )
    }

    override fun setSize(size: Int) {
        setEditTextSpan(
            AbsoluteSizeSpan(size),
            SpanStates.Spans.AbsoluteSizeSpan,
            absoluteSize = size
        )
    }

    override fun setUnderlined() {
        setEditTextSpan(UnderlineSpan(), SpanStates.Spans.UnderlineSpan)
    }

    override fun setStrikethrough() {
        setEditTextSpan(StrikethroughSpan(), SpanStates.Spans.StrikeThroughSpan)
    }

    override fun setColor(color: Any) {
        setEditTextSpan(
            when (color) {
                is Int -> ColorSpan(color)
                is String -> ColorSpan(color)
                else -> ColorSpan(Color.BLACK)
            }, SpanStates.Spans.ForegroundColorSpan, iColor = color
        )
    }

    /**
     * Indexing rich note for database to store
     *
     * @return [Pair]
     * <[Int],[String]>
     *
     * [Int] : A sort of number that from left to right market the [RichStates]
     *
     * [String] : Json of [RichStates]>
     */
    suspend fun indexRichNote(): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { co ->
                val richSer = arrayListOf<String>()
                var richStates = 0
                for (i in 0 until childCount) {
                    when (val tag = getChildAt(i).tag) {
                        is RichNoteStates -> richSer.add(
                            RichNoteSer(
                                getEdittext(i)?.text.toString(),
                                tag.spanStates.listToJson(SpanStates::class.java)
                            ).toJson()
                        ).apply { richStates = richStates * 10 + TEXT }
                        is RichVideoStates -> richSer.add(tag.toJson())
                            .apply { richStates = richStates * 10 + VIDEO }
                        is RichPhotoStates -> richSer.add(tag.toJson())
                            .apply { richStates = richStates * 10 + PHOTO }
                        is RichMusicStates -> richSer.add(tag.toJson())
                            .apply { richStates = richStates * 10 + MUSIC }
                    }
                }
                co.resumeWith(
                    Result.success(Pair(richStates, richSer.listToJson(String::class.java)))
                )
            }
        }
    }

    /**
     * Function used for insert different type of media that done the basic work
     *
     * @param func Callback used for custom
     */
    private inline fun insertMedia(func: (Int) -> Unit) {
        var insertPosition = curPosition
        if (!inIndex) {
            //While inserting a view,delete the empty edittext at the top of target(Image)
            if (isEditable && insertPosition in 0 until childCount) {
                val child = getChildAt(insertPosition)
                if (child is EditText && child.text.isEmpty()) {
                    removeView(child)
                    insertPosition--
                }
            }
        }
        func(insertPosition + if (!inIndex) 1 else 0)
        //Insert a edittext to make sure there have a place for input
        if (isEditable && !inIndex) insertText(
            RichNoteStates(
                "",
                arrayListOf()
            )
        )
    }

    /**
     * Basic function for set span and update [RichNoteStates]
     *
     * @param span Target span
     * @param targetSpan Enum that market the span
     * @param iColor Use for [ForegroundColorSpan] and [BackgroundColorSpan],[String] and [Int] are Supported
     * @param absoluteSize Use for [AbsoluteSizeSpan]
     * @param relativeSize Use for [RelativeSizeSpan]
     * @param scaleX Use for [ScaleXSpan]
     * @param style Use for [StyleSpan]
     * @param url Use for [URLSpan]
     */
    private fun setEditTextSpan(
        span: Any,
        targetSpan: SpanStates.Spans,
        iColor: Any? = null,
        absoluteSize: Int? = null,
        relativeSize: Float? = null,
        scaleX: Float? = null,
        style: Int? = null,
        url: String? = null
    ) {
        getEdittext(curPosition)?.also {
            it.text.setSpan(
                span,
                it.selectionStart,
                it.selectionEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            getCurRichStates().let { rs ->
                if (rs is RichNoteStates?) rs?.apply {
                    this.text = it.text
                    (spanStates as ArrayList)
                        .add(
                            SpanStates(
                                it.selectionStart,
                                it.selectionEnd,
                                targetSpan,
                                iColor,
                                absoluteSize,
                                relativeSize,
                                scaleX,
                                style,
                                url
                            )
                        )
                }
                getEdittext(curPosition)?.tag = rs
            }
        }
    }

    /**
     * Get edittext
     *
     * @param position target
     * @return [EditText] or null when target is not [EditText] or caught [IndexOutOfBoundsException]
     */
    private fun getEdittext(position: Int): EditText? {
        return try {
            getChildAt(position).let {
                if (it is EditText) it else null
            }
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    /**
     * Get current [RichStates]
     *
     * @return [RichStates] or null when caught [IndexOutOfBoundsException]
     */
    private fun getCurRichStates(): RichStates? {
        return try {
            when (getChildAt(curPosition).tag) {
                is RichStates -> getChildAt(curPosition).tag as RichStates
                else -> null
            }
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    var iRichListener: IRichListener? = null

    interface IRichListener {
        fun play(uri: Uri, progress: Long)
        fun pause()
        fun jumpTo(note: String)
        fun openImage(uri: Uri, name: String)
    }
}
