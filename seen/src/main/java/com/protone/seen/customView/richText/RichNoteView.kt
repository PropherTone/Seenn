package com.protone.seen.customView.richText

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Bitmap
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
import com.protone.api.saveToFile
import com.protone.api.toBitmap
import com.protone.mediamodle.note.MyTextWatcher
import com.protone.mediamodle.note.entity.*
import com.protone.mediamodle.note.spans.ColorSpan
import com.protone.mediamodle.note.spans.ISpanForEditor
import com.protone.seen.R
import com.protone.seen.customView.MyMusicPlayer
import com.protone.seen.databinding.RichMusicLayoutBinding
import com.protone.seen.databinding.RichPhotoLayoutBinding
import com.protone.seen.databinding.VideoCardBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow

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
        addView(when (isEditable) {
            true ->
                EditText(context).apply {
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    background = null
                    setText(note.text)
                    setOnKeyListener { _, keyCode, event ->
                        //Noticed if no text to delete when delete key pressed
                        if (keyCode == KeyEvent.KEYCODE_DEL &&
                            event.action == KeyEvent.ACTION_DOWN &&
                            text.isEmpty()
                        ) {
                            var indexOfChild = indexOfChild(this@apply)
                            if (indexOfChild > 0) {
                                removeViewAt(indexOfChild)
                                //Delete next view if it's not an edittext
                                if (indexOfChild-- > 0 && getChildAt(indexOfChild) !is EditText) {
                                    removeView(getChildAt(indexOfChild--))
                                    //Insert new edittext when there is no input place
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
                            if (!inIndex) curPosition = this@RichNoteView.indexOfChild(this@apply)
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
                            if (!inIndex) curPosition = this@RichNoteView.indexOfChild(this)
                        }
                    }
                    setOnClickListener {
                        if (!inIndex) curPosition = this@RichNoteView.indexOfChild(this)
                    }
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
        addView(VideoCardBinding.inflate(context.layoutInflater, this, false).apply {
            videoPlayer.setVideoPath(video.uri)
            videoPlayer.setFullScreen {
                iRichListener?.open(video.uri, "", true)
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

    private fun getBitmapWH(dba: Bitmap): IntArray? {
        return try {
            val height = dba.height
            val width = dba.width
            val index = width / height
            val bmH = Config.screenWidth / index
            intArrayOf(Config.screenWidth, bmH)
        } catch (e: Exception) {
            null
        }
    }

    private fun getWHFromPath(path: String?): IntArray? {
        return try {
            val option = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val dba = BitmapFactory.decodeFile(path, option) ?: return null
            val height = dba.height
            val width = dba.width
            val index = width / height
            val bmH = Config.screenWidth / index
            dba.recycle()
            intArrayOf(Config.screenWidth, bmH)
        } catch (e: Exception) {
            null
        }
    }

    override fun insertImage(photo: RichPhotoStates) = insertMedia {
        addView(RichPhotoLayoutBinding.inflate(context.layoutInflater, this, false).apply {
            val ba = photo.uri.toBitmap()
            if (ba == null && photo.path == null) {
                Glide.with(context).asDrawable()
                    .load(R.drawable.ic_baseline_error_outline_24_black)
                    .into(this.richPhotoIv)
            } else if (photo.path != null) {
                ba?.recycle()
                val bitmapWH = getWHFromPath(photo.path)
                Glide.with(context).asDrawable()
                    .load(photo.path).error(R.drawable.ic_baseline_error_outline_24_black)
                    .let { glide ->
                        if (bitmapWH != null) glide.override(bitmapWH[0], bitmapWH[1]) else glide
                    }.into(this.richPhotoIv)
            } else if (ba != null) {
                val bitmapWH = getBitmapWH(ba)
                Glide.with(context).asDrawable().load(photo.uri)
                    .error(R.drawable.ic_baseline_error_outline_24_black).let { glide ->
                    if (bitmapWH != null) glide.override(bitmapWH[0], bitmapWH[1]) else glide
                }.into(this.richPhotoIv)
                richPhotoTitle.text = photo.name
                richPhotoDetail.text = photo.date
            }
            richPhotoFull.setOnClickListener { iRichListener?.open(photo.uri, photo.name, false) }
            richPhotoTvContainer.setOnClickListener {
                richPhotoTitle.apply {
                    isVisible = !isVisible
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
     * [Int] : A sort of number that mark the [RichStates] from left to right
     *
     * [String] : Json of [RichStates]>
     */
    suspend fun indexRichNote(title: String): Pair<Int, String> {
        val richArray = arrayOfNulls<String>(childCount)
        val richSer = arrayListOf<String>()
        var richStates = 0
        val taskChannel = Channel<Pair<String, Int>>(Channel.UNLIMITED)
        var count = 0
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine {
                async(Dispatchers.IO) {
                    while (isActive) {
                        taskChannel.receiveAsFlow().collect {
                            if (it.second < childCount) {
                                richArray[it.second] = it.first
                                count++
                            }
                            if (count >= childCount) {
                                taskChannel.close()
                            }
                        }
                        richArray.onEach {
                            it?.let { state ->
                                richSer.add(state)
                            }
                        }
                        it.resumeWith(
                            Result.success(Pair(richStates, richSer.listToJson(String::class.java)))
                        )
                        break
                    }
                }.start()
                for (i in 0 until childCount) {
                    when (val tag = getChildAt(i).tag) {
                        is RichNoteStates -> {
                            richStates = richStates * 10 + TEXT
                            taskChannel.offer(
                                Pair(
                                    RichNoteSer(
                                        getEdittext(i)?.text.toString(),
                                        tag.spanStates.listToJson(SpanStates::class.java)
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
                            val child = getChildAt(i)
                            async(Dispatchers.IO) {
                                tag.uri.toBitmap(child.measuredWidth, child.measuredHeight)
                                    ?.saveToFile(title + "_${System.currentTimeMillis()}")
                                    ?.let {
                                        if (tag.path == null) {
                                            tag.path = it
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
     * Done the basic work for insert different type of media
     *
     * @param func Callback used for custom
     */
    private inline fun insertMedia(func: (Int) -> Unit) {
        var insertPosition = curPosition
        if (!inIndex) {
            //While inserting a view,delete the empty edittext at the top of the target(Image)
            if (isEditable && insertPosition in 0 until childCount) {
                val child = getChildAt(insertPosition)
                if (child is EditText && child.text.isEmpty()) {
                    removeView(child)
                    insertPosition--
                }
            }
        }
        var index = insertPosition + if (!inIndex) 1 else 0
        if (index >= childCount) index = childCount
        func(index)
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
        fun open(uri: Uri, name: String, isVideo: Boolean)
    }
}
