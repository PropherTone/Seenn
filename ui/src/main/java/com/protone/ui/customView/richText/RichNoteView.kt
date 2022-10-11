package com.protone.ui.customView.richText

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.*
import com.protone.api.note.IRichNoteImageLoader
import com.protone.api.note.MyTextWatcher
import com.protone.api.spans.ISpanForEditor
import com.protone.api.spans.SpanStates
import com.protone.ui.customView.musicPlayer.BaseMusicPlayer
import com.protone.ui.databinding.RichMusicLayoutBinding
import com.protone.ui.databinding.RichPhotoLayoutBinding
import com.protone.ui.databinding.VideoCardBinding

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

    private var curPlaying = 0
    private var isPlaying = false

    var iRichListener: IRichListener? = null

    private val noteHandler = RichNoteHandler(this)

    init {
        orientation = VERTICAL
        layoutTransition = LayoutTransition()
    }

    /**
     * Generate rich text
     *
     * @param list List of [RichStates] use for generating
     */
    suspend fun setRichList(list: List<RichStates>) {
        removeAllViews()
        noteHandler.setRichList(list)
    }

    suspend fun setRichList(richCode: Int, text: String) {
        noteHandler.setRichList(richCode, text)
    }

    suspend fun indexRichNote(
        title: String,
        onSaveResult: suspend (ArrayList<Uri>) -> Boolean
    ): Pair<Int, String> = noteHandler.indexRichNote(title, childCount, onSaveResult)

    fun setImageLoader(loader: IRichNoteImageLoader) {
        noteHandler.imageLoader = loader
    }

    fun setMusicProgress(progress: Long) {
        getChildAt(curPlaying)?.let {
            if (it is BaseMusicPlayer) {
                it.progress?.barSeekTo(progress)
            }
        }
    }

    fun setMusicDuration(duration: Long) {
        noteHandler.getCurrentChild()?.let {
            if (it is BaseMusicPlayer) {
                it.duration = duration
            }
        }
    }

    fun toPicture(): Bitmap {
        val bitmap =
            Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }

    fun getSelectionTextSize() =
        noteHandler.getCurrentEditText()?.let {
            val spans = it.text.getSpans(
                it.selectionStart,
                it.selectionEnd,
                AbsoluteSizeSpan::class.java
            )
            if (spans.isNotEmpty() && spans.size == 1) {
                spans[0].size
            } else it.textSize.toInt()
        } ?: 20

    override fun insertText(note: RichNoteStates) {
        addView(when (isEditable) {
            true ->
                EditText(context).apply {
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    textSize = 18f
                    setTextColor(Color.BLACK)
                    background = null
                    inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    setSpannableFactory(object : Spannable.Factory() {
                        override fun newSpannable(source: CharSequence?): Spannable {
                            return SpannableStringBuilder(source)
                        }
                    })
                    setText(note.text, TextView.BufferType.SPANNABLE)
                    setOnKeyListener { _, keyCode, event ->
                        //Noticed if no text to delete when delete key pressed
                        if (keyCode == KeyEvent.KEYCODE_DEL &&
                            event.action == KeyEvent.ACTION_DOWN &&
                            text.isEmpty()
                        ) {
                            var indexOfChild = indexOfChild(this@apply)
                            //Delete next view if it's not an edittext
                            if (--indexOfChild > 0 && getChildAt(indexOfChild) !is EditText) {
                                removeView(getChildAt(indexOfChild))

                                getChildAt(indexOfChild)?.let {
                                    if (it is ImageView) {
                                        if (it.tag != null && it.tag is RichPhotoStates) {
                                            (it.tag as RichPhotoStates).path?.let { path ->
                                                noteHandler.addDeleteMedias(path)
                                            }
                                        }
                                    }
                                }
                                //Insert new edittext when there is no input place
                                if (--indexOfChild >= 0 && getChildAt(indexOfChild) !is EditText || childCount <= 0) {
                                    insertText(RichNoteStates(""))
                                }
                            }

                        }
                        false
                    }
                    addTextChangedListener(object : MyTextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            noteHandler.updatePosition(this@RichNoteView.indexOfChild(this@apply))
                            noteHandler.getCurRichStates().let {
                                if (it is RichNoteStates) it.apply { text = s }
                                tag = it
                            }
                        }
                    })
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            noteHandler.updatePosition(this@RichNoteView.indexOfChild(this@apply))
                        }
                    }
                    setOnClickListener {
                        noteHandler.updatePosition(this@RichNoteView.indexOfChild(this@apply))
                    }
                    setOnTouchListener { _, _ ->
                        iRichListener?.onContentGainedFocus()
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
                textSize = 18f
                setTextColor(Color.BLACK)
                setSpannableFactory(object : Spannable.Factory() {
                    override fun newSpannable(source: CharSequence?): Spannable {
                        return SpannableStringBuilder(source)
                    }
                })
                setText(note.text, TextView.BufferType.SPANNABLE)
                text = note.text
            }
        }).run {
            this@RichNoteView.requestFocus()
            noteHandler.getCurrentEditText()?.also {
                it.requestFocus()
                it.performClick()
            }
        }
    }

    /**
     * Do the basic work for insert different type of media
     *
     * @param func Callback used for custom
     */
    private inline fun insertMedia(func: (Int) -> Unit) {
        var insertPosition = noteHandler.curPosition
        if (!noteHandler.inIndex) {
            //While inserting a view,delete the empty edittext at the top of the target(Image)
            if (isEditable && insertPosition in 0 until childCount) {
                val child = getChildAt(insertPosition)
                if (child is EditText && child.text.isEmpty()) {
                    removeView(child)
                    insertPosition--
                }
            }
        }
        var index = insertPosition + if (!noteHandler.inIndex) 1 else 0
        if (index >= childCount) index = childCount
        func(index)
        //Insert a edittext to make sure there have a place for input
        if (isEditable && !noteHandler.inIndex) insertText(RichNoteStates(""))
    }

    override fun insertVideo(video: RichVideoStates) = insertMedia {
        addView(VideoCardBinding.inflate(context.newLayoutInflater, this, false).apply {
            videoPlayer.setVideoPath(video.uri)
            videoPlayer.setFullScreen {
                iRichListener?.open(video.uri, "", true)
            }
        }.root.also { r -> r.tag = video }, it)
    }

    override fun insertMusic(music: RichMusicStates) = insertMedia {
        addView(RichMusicLayoutBinding.inflate(context.newLayoutInflater, this, false).apply {
            richMusic.control.setOnClickListener {
                if (isPlaying) {
                    iRichListener?.play(music.uri, richMusic.progress.barDuration)
                } else {
                    iRichListener?.pause()
                }
                curPlaying = this@RichNoteView.indexOfChild(root)
            }
            richMusic.next.isGone = true
            richMusic.previous.isGone = true
            richMusic.looper?.isGone = true
            richLinkContainer.isGone = music.link == null
            if (!isEditable) {
                richLinkContainer.setOnClickListener {
                    music.link?.let { note -> iRichListener?.jumpTo(note) }
                }
                richLink.text = music.link ?: "".also {
                    richLink.isGone = true
                }
            }
            richMusic.cover = music.uri
            richMusic.setName(music.name)
        }.root.also { r -> r.tag = music }, it)
    }

    override fun getChild(index: Int): View? = getChildAt(index)

    override fun insertImage(photo: RichPhotoStates, ba: Bitmap?) = insertMedia {
        addView(RichPhotoLayoutBinding.inflate(context.newLayoutInflater, this, false).apply {
            if (ba == null && photo.path == null) {
                noteHandler.imageLoader?.loadError(context, this.richPhotoIv)
            } else if (photo.path != null) {
                ba?.recycle()
                noteHandler.imageLoader?.loadImage(context, photo.path, this.richPhotoIv)
            } else if (ba != null) {
                noteHandler.imageLoader?.loadImage(context, ba, this.richPhotoIv)
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
        noteHandler.setEditTextSpan(SpanStates.Spans.Style(Typeface.BOLD))
    }

    override fun setItalic() {
        noteHandler.setEditTextSpan(SpanStates.Spans.Style(Typeface.ITALIC))
    }

    override fun setSize(size: Int) {
        noteHandler.setEditTextSpan(SpanStates.Spans.AbsoluteSize(size))
    }

    override fun setUnderlined() {
        noteHandler.setEditTextSpan(SpanStates.Spans.Underline)
    }

    override fun setStrikethrough() {
        noteHandler.setEditTextSpan(SpanStates.Spans.StrikeThrough)
    }

    override fun setURL(url: String) {
        noteHandler.setEditTextSpan(SpanStates.Spans.URL(url))
    }

    override fun setSubscript() {
        noteHandler.setEditTextSpan(SpanStates.Spans.Subscript)
    }

    override fun setSuperscript() {
        noteHandler.setEditTextSpan(SpanStates.Spans.Superscript)
    }

    override fun setColor(color: Any) {
        noteHandler.setEditTextSpan(SpanStates.Spans.ForegroundColor(color))
    }

    override fun setBackColor(color: Any) {
        noteHandler.setEditTextSpan(SpanStates.Spans.BackgroundColor(color))
    }

    interface IRichListener {
        fun onContentGainedFocus()
        fun play(uri: Uri, progress: Long)
        fun pause()
        fun jumpTo(note: String)
        fun open(uri: Uri, name: String, isVideo: Boolean)
    }

    open class IRichListenerImp : IRichListener {
        override fun onContentGainedFocus() = Unit
        override fun play(uri: Uri, progress: Long) = Unit
        override fun pause() = Unit
        override fun jumpTo(note: String) = Unit
        override fun open(uri: Uri, name: String, isVideo: Boolean) = Unit
    }
}
