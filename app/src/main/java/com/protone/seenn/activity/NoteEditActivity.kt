package com.protone.seenn.activity

import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.*
import com.protone.api.context.*
import com.protone.api.entity.*
import com.protone.api.json.listToJson
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.json.toUri
import com.protone.seenn.R
import com.protone.seenn.databinding.NoteEditActivityBinding
import com.protone.ui.customView.richText.RichNoteImageLoader
import com.protone.ui.customView.richText.note.spans.ISpanForUse
import com.protone.ui.dialog.imageListDialog
import com.protone.ui.popWindows.ColorfulPopWindow
import com.protone.worker.viewModel.GalleryViewModel
import com.protone.worker.viewModel.NoteEditViewModel
import com.protone.worker.viewModel.NoteViewViewModel
import com.protone.worker.viewModel.PickMusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NoteEditActivity :
    BaseActivity<NoteEditActivityBinding, NoteEditViewModel, NoteEditViewModel.NoteEvent>(true),
    ISpanForUse {
    override val viewModel: NoteEditViewModel by viewModels()

    private var listPopWindow: ColorfulPopWindow? = null
    private var numberPopWindow: ColorfulPopWindow? = null
    private var colorPopWindow: ColorfulPopWindow? = null

    private var title: String
        set(value) {
            binding.noteEditTitle.setText(value)
        }
        get() = binding.noteEditTitle.text.toString()

    override fun createView(): NoteEditActivityBinding {
        return NoteEditActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@NoteEditActivity
            noteEditToolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                toolbar.progress =
                    -verticalOffset / appBarLayout.totalScrollRange.toFloat()
            }
            setSoftInputStatusListener { height, isShow ->
                if (isShow) {
                    root.marginBottom(height)
                } else {
                    root.marginBottom(0)
                }
            }
            noteEditRichNote.isEditable = true
            noteEditRichNote.setImageLoader(RichNoteImageLoader())
        }
    }

    override suspend fun NoteEditViewModel.init() {
        binding.noteEditRichNote.setRichList(listOf(RichNoteStates("", arrayListOf())))
        val contentTitle = intent.getStringExtra(NoteEditViewModel.CONTENT_TITLE)
        val noteName = intent.getStringExtra(NoteEditViewModel.NOTE)
        if (contentTitle != null) {
            title = contentTitle
            initEditor(
                1,
                mutableListOf(
                    RichNoteSer(
                        try {
                            getGainData<String>()
                        } catch (e: Exception) {
                            null
                        } ?: R.string.none.getString(),
                        arrayListOf<SpanStates>().listToJson(SpanStates::class.java)
                    ).toJson()
                ).listToJson(String::class.java)
            )
        } else {
            noteByName = noteName?.let {
                getNoteByName(it)?.let { n ->
                    withContext(Dispatchers.IO) {
                        if (n.imagePath == null) return@withContext
                        val file = File(n.imagePath)
                        if (file.isFile) {
                            setNoteIcon(n.imagePath)
                        } else {
                            iconUri = n.imagePath.toUri()
                            setNoteIcon(iconUri)
                        }
                    }
                    title = n.title
                    initEditor(n.richCode, n.text)
                    onEdit = true
                    n
                }
            }
        }

        onViewEvent {
            when (it) {
                NoteEditViewModel.NoteEvent.Confirm -> confirm()
                NoteEditViewModel.NoteEvent.PickIcon -> pickIcon()
                NoteEditViewModel.NoteEvent.PickImage -> pickImage()
                NoteEditViewModel.NoteEvent.PickVideo -> pickVideo()
                NoteEditViewModel.NoteEvent.PickMusic -> pickMusic()
            }
        }
    }

    override suspend fun doResume() {
        binding.root.marginBottom(0)
        isKeyBroadShow = false
    }

    private suspend fun NoteEditViewModel.pickIcon() {
        startGalleryPick(true)?.let { re ->
            if (noteByName != null) {
                noteByName?.imagePath?.deleteFile()
            }
            iconUri = re.uri
            setNoteIcon(re.uri)
        }
    }

    private suspend fun pickVideo() {
        startGalleryPick(false)?.let { re ->
            insertVideo(re.uri)
        }
    }

    private suspend fun NoteEditViewModel.pickMusic() {
        startActivityForResult(
            PickMusicActivity::class.intent.apply {
                putExtra(PickMusicViewModel.MODE, PickMusicViewModel.PICK_MUSIC)
            }
        )?.also { re ->
            re.data?.data?.let { uri ->
                if (allNote == null) allNote = getAllNote()
                insertMusic(uri, allNote!!, getMusicTitle(uri))
            }
        }
    }

    private suspend fun NoteEditViewModel.pickImage() {
        startGalleryPick(true)?.let { re ->
            insertImage(
                RichPhotoStates(
                    re.uri, re.name, null,
                    re.date.toDateString().toString()
                )
            )
            medias.add(re)
        }
    }

    private suspend fun NoteEditViewModel.confirm() {
        if (title.isEmpty()) {
            R.string.enter_title.getString().toast()
            return
        }
        showProgress(true)
        val checkedTitle = if (onEdit) title else checkNoteTitle(title)
        val indexedRichNote = binding.noteEditRichNote.indexRichNote(checkedTitle) {
            if (it.size <= 0) return@indexRichNote true
            return@indexRichNote imageListDialog(it)
        }
        val note = Note(
            checkedTitle,
            indexedRichNote.second,
            null,
            System.currentTimeMillis(),
            indexedRichNote.first
        )
        if (onEdit) {
            if (intent.getStringExtra(NoteEditViewModel.NOTE) == null) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
            val inNote = noteByName
            if (inNote == null) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
            copyNote(inNote, note)
            inNote.imagePath = if (iconUri != null) saveIcon(checkedTitle) else inNote.imagePath
            val re = updateNote(inNote)
            if (re == null && re == -1) {
                insertNote(
                    inNote,
                    intent.getStringExtra(NoteEditViewModel.NOTE_DIR)
                ).let { result ->
                    if (result) {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(NoteViewViewModel.NOTE_NAME, inNote.title)
                        )
                        finish()
                    } else R.string.failed_msg.getString().toast()
                }
            } else {
                showProgress(false)
                setResult(RESULT_OK)
                finish()
            }
        } else if (insertNote(
                note.apply {
                    imagePath = saveIcon(checkedTitle)
                },
                intent.getStringExtra(NoteEditViewModel.NOTE_DIR)
            )
        ) {
            finish()
        } else R.string.failed_msg.getString().toast()
    }

    private suspend fun startGalleryPick(isPhoto: Boolean) =
        startActivityForResult(GalleryActivity::class.intent.apply {
            putExtra(
                GalleryViewModel.CHOOSE_MODE,
                if (isPhoto) GalleryViewModel.CHOOSE_PHOTO else GalleryViewModel.CHOOSE_VIDEO
            )
        })?.let { re ->
            re.data?.getStringExtra(GalleryViewModel.Gallery_DATA)
                ?.toEntity(GalleryMedia::class.java)
        }


    private suspend fun initEditor(richCode: Int, text: String) = withContext(Dispatchers.Main) {
        binding.noteEditRichNote.setRichList(richCode, text)
    }

    override fun setBold() = binding.noteEditRichNote.setBold()

    override fun setItalic() = binding.noteEditRichNote.setItalic()

    override fun setSize() {
        binding.run {
            if (numberPopWindow != null) {
                numberPopWindow?.dismiss()
                numberPopWindow = null
            } else ColorfulPopWindow(this@NoteEditActivity).also {
                numberPopWindow = it
                it.setOnDismissListener { numberPopWindow = null }
            }.startNumberPickerPopup(
                noteEditTool,
                noteEditRichNote.getSelectionTextSize()
            ) { noteEditRichNote.setSize(it) }
        }
    }

    private suspend fun insertImage(photo: RichPhotoStates) =
        binding.noteEditRichNote.insertImage(photo, photo.uri.toBitmap())

    private fun insertVideo(uri: Uri) {
        binding.noteEditRichNote.insertVideo(RichVideoStates(uri, null, name = ""))
    }

    private fun insertMusic(uri: Uri, list: MutableList<String>, title: String) {
        if (listPopWindow != null) {
            listPopWindow?.dismiss()
            listPopWindow = null
        } else ColorfulPopWindow(this).also {
            listPopWindow = it
            it.setOnDismissListener { listPopWindow = null }
        }.startListPopup(R.string.pick_note.getString(), binding.noteEditTool, list) {
            listPopWindow?.dismiss()
            binding.noteEditRichNote.insertMusic(RichMusicStates(uri, it, title))
        }
    }

    private suspend fun setNoteIcon(uri: Uri?) = withContext(Dispatchers.Main) {
        Glide.with(this@NoteEditActivity).asDrawable().load(uri).into(binding.noteEditIcon)
    }

    private suspend fun setNoteIcon(path: String?) = withContext(Dispatchers.Main) {
        Glide.with(this@NoteEditActivity).asDrawable().load(path).into(binding.noteEditIcon)
    }

    private suspend fun showProgress(isShow: Boolean) = withContext(Dispatchers.Main) {
        (binding.toolbar.getViewById(R.id.noteEdit_progress) as ImageView?)?.apply {
            drawable.let {
                when (it) {
                    is Animatable ->
                        if (isShow) it.start().also { isVisible = true }
                        else it.stop().also { changeIconAni(binding.noteEditProgress) }
                }
            }
        }
    }

    private suspend fun changeIconAni(view: ImageView) = withContext(Dispatchers.Main) {
        AnimationHelper.apply {
            animatorSet(scaleX(view, 0f), scaleY(view, 0f), doOnEnd = {
                view.setImageDrawable(R.drawable.ic_baseline_check_24.getDrawable())
                animatorSet(scaleX(view, 1f), scaleY(view, 1f), play = true, doOnEnd = {
                    alpha(view, 0f, play = true, doOnEnd = { view.isVisible = false })
                })
            }, play = true)
        }
    }

    override fun setUnderlined() = binding.noteEditRichNote.setUnderlined()
    override fun setStrikethrough() = binding.noteEditRichNote.setStrikethrough()
    override fun insertImage() = Unit
    override fun insertVideo() = Unit
    override fun insertMusic() = Unit
    override fun setColor() {
        if (colorPopWindow != null) {
            colorPopWindow?.dismiss()
            colorPopWindow = null
        } else ColorfulPopWindow(this).also {
            colorPopWindow = it
            it.setOnDismissListener { colorPopWindow = null }
        }.startColorPickerPopup(binding.noteEditTool) {
            binding.noteEditRichNote.setColor(it.toHexColor())
        }
    }
}