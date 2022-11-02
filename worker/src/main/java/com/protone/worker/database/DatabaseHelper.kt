package com.protone.worker.database

import android.content.Context
import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.baseType.withIOContext
import com.protone.api.entity.*
import com.protone.database.R
import com.protone.database.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DatabaseHelper {

    private val executorService by lazy { CoroutineScope(Dispatchers.IO) }
    fun getScope() = executorService

    private val _mediaNotifier = MutableSharedFlow<MediaAction>()
    val mediaNotifier = _mediaNotifier.asSharedFlow()

    companion object {
        @JvmStatic
        val instance: DatabaseHelper
            @Synchronized get() {
                if (helperImpl == null) {
                    synchronized(this::class) {
                        helperImpl = DatabaseHelper()
                    }
                }
                return helperImpl!!
            }

        private var helperImpl: DatabaseHelper? = null
    }

    val musicBucketDAOBridge by lazy { MusicBucketDAOBridge() }
    val musicDAOBridge by lazy { MusicDAOBridge() }
    val signedGalleryDAOBridge by lazy { GalleryDAOBridge() }
    val noteDAOBridge by lazy { NoteDAOBridge() }
    val noteDirDAOBridge by lazy { NoteTypeDAOBridge() }
    val galleryBucketDAOBridge by lazy { GalleryBucketDAOBridge() }
    val galleriesWithNotesDAOBridge by lazy { GalleriesWithNotesDAOBridge() }
    val noteDirWithNoteDAOBridge by lazy { NoteDirWithNoteDAOBridge() }
    val musicWithMusicBucketDAOBridge by lazy { MusicWithMusicBucketDAOBridge() }

    fun showDataBase(context: Context) {
        showRoomDB(context)
    }

    fun shutdownNow() {
        if (executorService.isActive) {
            executorService.cancel()
        }
        shutdownDataBase()
    }

    fun pollEvent(mediaAction: MediaAction) {
        execute(Dispatchers.Default) {
            _mediaNotifier.emit(mediaAction)
        }
    }

    inline fun execute(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        crossinline runnable: suspend () -> Unit
    ): Job = getScope().launch(dispatcher) {
        try {
            runnable.invoke()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            R.string.unknown_error.getString().toast()
        } finally {
            cancel()
        }
    }

    inner class MusicBucketDAOBridge : BaseMusicBucketDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun updateMusicBucketAsync(bucket: MusicBucket) {
            execute {
                updateMusicBucket(bucket)
            }
        }

        fun addMusicBucketAsync(musicBucket: MusicBucket) {
            execute {
                addMusicBucket(musicBucket)
            }
        }

        fun deleteMusicBucketAsync(bucket: MusicBucket) {
            execute {
                deleteMusicBucket(bucket)
            }
        }

        suspend fun deleteMusicBucketRs(bucket: MusicBucket): Boolean {
            deleteMusicBucket(bucket)
            return getMusicBucketByName(bucket.name) == null
        }

        inline fun addMusicBucketWithCallBack(
            musicBucket: MusicBucket,
            crossinline callBack: suspend (result: Boolean, name: String) -> Unit
        ) {
            execute {
                var count = 0
                val tempName = musicBucket.name
                val names = mutableMapOf<String, Int>()
                getAllMusicBucket()?.forEach {
                    names[it.name] = 1
                    if (it.name == musicBucket.name) {
                        musicBucket.name = "${tempName}(${++count})"
                    }
                }
                while (names[musicBucket.name] != null) {
                    musicBucket.name = "${tempName}(${++count})"
                }
                addMusicBucket(musicBucket)
                callBack(getMusicBucketByName(musicBucket.name) != null, musicBucket.name)
            }
        }

    }

    inner class MusicDAOBridge : BaseMusicDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun insertMusicMultiAsync(music: List<Music>) {
            execute {
                music.forEach {
                    insertMusic(it)
                }
            }
        }


        suspend fun insertMusicMulti(music: List<Music>) {
            music.forEach {
                insertMusic(it)
            }
        }

        fun deleteMusicMultiAsync(music: List<Music>) {
            if (music.isEmpty()) return
            execute {
                music.forEach {
                    deleteMusic(it)
                }
            }
        }

        suspend fun deleteMusicMulti(music: List<Music>) {
            if (music.isEmpty()) return
            music.forEach {
                deleteMusic(it)
            }
        }

        fun insertMusicCheck(music: Music) = execute {
            getMusicByUri(music.uri).let {
                if (it == null) {
                    insertMusic(music)
                } else {
                    updateMusic(music)
                }
            }
        }

        fun deleteMusicAsync(music: Music) = execute {
            deleteMusic(music)
        }

    }

    inner class GalleryDAOBridge : BaseGalleryDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun deleteSignedMediaMultiAsync(list: MutableList<GalleryMedia>) {
            execute {
                list.forEach {
                    deleteSignedMediaByUri(it.uri)
                }
            }
        }

        fun deleteSignedMediaAsync(media: GalleryMedia) {
            execute {
                deleteSignedMedia(media)
            }
        }

        fun updateMediaMultiAsync(list: MutableList<GalleryMedia>) {
            execute { list.forEach { updateSignedMedia(it) } }
        }

        suspend fun insertSignedMediaChecked(media: GalleryMedia): GalleryMedia? {
            val it = getSignedMedia(media.uri)
            return if (it != null) {
                if (it == media) return null
                updateSignedMedia(it)
                it
            } else {
                insertSignedMedia(media)
                media
            }
        }

    }

    inner class NoteDAOBridge : BaseNoteDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun deleteNoteAsync(note: Note) {
            execute {
                deleteNote(note)
            }
        }

        suspend fun insertNoteRs(note: Note): Pair<Boolean, Long> {
            val id = insertNote(note)
            return Pair(getNoteByName(note.title) != null, id)
        }
    }

    inner class NoteTypeDAOBridge : BaseNoteTypeDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        suspend fun insertNoteDirRs(
            noteDir: NoteDir,
        ): Pair<Boolean, String> {
            var count = 0
            val tempName = noteDir.name
            val names = mutableMapOf<String, Int>()
            getALLNoteDir()?.forEach { dir ->
                names[dir.name] = 1
                if (dir.name == noteDir.name) {
                    noteDir.name = "${tempName}(${++count})"
                }
            }
            while (names[noteDir.name] != null) {
                noteDir.name = "${tempName}(${++count})"
            }
            insertNoteDir(noteDir)
            return Pair(getNoteDir(noteDir.name) != null, noteDir.name)
        }

        suspend fun doDeleteNoteDirRs(noteDir: NoteDir): Boolean {
            deleteNoteDir(noteDir)
            return getNoteDir(noteDir.name) != null
        }

    }

    inner class GalleryBucketDAOBridge : BaseGalleryBucketDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun deleteGalleryBucketAsync(galleryBucket: GalleryBucket) {
            execute {
                deleteGalleryBucket(galleryBucket)
            }
        }

        inline fun insertGalleryBucketCB(
            galleryBucket: GalleryBucket,
            crossinline callBack: suspend (Boolean, String) -> Unit
        ) {
            execute {
                var count = 0
                val tempName = galleryBucket.type
                val names = mutableMapOf<String, Int>()
                getAllGalleryBucket(galleryBucket.isImage)?.forEach {
                    names[it.type] = 1
                    if (it.type == galleryBucket.type) {
                        galleryBucket.type = "${tempName}(${++count})"
                    }
                }
                while (names[galleryBucket.type] != null) {
                    galleryBucket.type = "${tempName}(${++count})"
                }
                insertGalleryBucket(galleryBucket)
                callBack.invoke(getGalleryBucket(galleryBucket.type) != null, galleryBucket.type)
            }
        }

    }

    inner class MusicWithMusicBucketDAOBridge : BaseMusicWithMusicBucketDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun deleteMusicWithMusicBucketAsync(musicID: Long, musicBucketId: Long) {
            execute {
                deleteMusicWithMusicBucket(musicID, musicBucketId)
            }
        }

        fun insertMusicMultiAsyncWithBucket(musicBucket: String, music: List<Music>) {
            execute {
                val bucket = musicBucketDAOBridge.getMusicBucketByName(musicBucket)
                bucket?.let { b ->
                    music.forEach {
                        insertMusicWithMusicBucket(
                            MusicWithMusicBucket(
                                b.musicBucketId,
                                it.musicBaseId
                            )
                        )
                    }
                }
            }
        }

        suspend fun insertMusicWithMusicBucket(musicID: Long, bucket: String): Long {
            val musicBucket = musicBucketDAOBridge.getMusicBucketByName(bucket) ?: return -1L
            val entity = MusicWithMusicBucket(
                musicBucket.musicBucketId,
                musicID
            )
            return (insertMusicWithMusicBucket(entity) ?: -1)
        }

    }

    inner class GalleriesWithNotesDAOBridge : BaseGalleriesWithNotesDAO() {
        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }
    }

    inner class NoteDirWithNoteDAOBridge : BaseNoteDirWithNoteDAO() {
        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }
    }

    sealed class BaseMusicBucketDAO : BaseDao() {

        private val musicBucketDAO = getMusicBucketDAO()

        suspend fun getAllMusicBucket(): List<MusicBucket>? = withIOContext {
            musicBucketDAO.getAllMusicBucket()
        }

        suspend fun getMusicBucketByName(name: String): MusicBucket? = withIOContext {
            musicBucketDAO.getMusicBucketByName(name)
        }

        suspend fun addMusicBucket(musicBucket: MusicBucket) = withIOContext {
            sendEvent(MediaAction.OnNewMusicBucket(musicBucket))
            musicBucketDAO.addMusicBucket(musicBucket)
        }

        suspend fun updateMusicBucket(musicBucket: MusicBucket): Int = withIOContext {
            sendEvent(MediaAction.OnMusicBucketUpdated(musicBucket))
            musicBucketDAO.updateMusicBucket(musicBucket)
        }

        suspend fun deleteMusicBucket(musicBucket: MusicBucket) = withIOContext {
            sendEvent(MediaAction.OnMusicBucketDeleted(musicBucket))
            musicBucketDAO.deleteMusicBucket(musicBucket)
        }

    }

    sealed class BaseMusicDAO : BaseDao() {

        private val musicDAO = getMusicDAO()

        suspend fun getAllMusic(): List<Music>? =
            withIOContext { musicDAO.getAllMusic() }

        suspend fun getMusicByUri(uri: Uri): Music? =
            withIOContext { musicDAO.getMusicByUri(uri) }

        suspend fun insertMusic(music: Music) = withIOContext {
            sendEvent(MediaAction.OnMusicInserted(music))
            musicDAO.insertMusic(music)
        }

        suspend fun deleteMusic(music: Music) = withIOContext {
            sendEvent(MediaAction.OnMusicDeleted(music))
            musicDAO.deleteMusic(music)
        }

        suspend fun updateMusic(music: Music): Int = withIOContext {
            sendEvent(MediaAction.OnMusicUpdate(music))
            musicDAO.updateMusic(music)
        }

    }

    sealed class BaseGalleryDAO : BaseDao() {

        private val signedGalleryDAO = getGalleryDAO()

        suspend fun getAllSignedMedia(): List<GalleryMedia>? = withIOContext {
            signedGalleryDAO.getAllSignedMedia()
        }

        suspend fun getAllMediaByType(isVideo: Boolean): List<GalleryMedia>? =
            withIOContext {
                signedGalleryDAO.getAllMediaByType(isVideo)
            }

        suspend fun getAllGallery(isVideo: Boolean): List<String>? = withIOContext {
            signedGalleryDAO.getAllGallery(isVideo)
        }

        suspend fun getAllGallery(): List<String>? = withIOContext {
            signedGalleryDAO.getAllGallery()
        }

        suspend fun getAllMediaByGallery(name: String, isVideo: Boolean): List<GalleryMedia>? =
            withIOContext {
                signedGalleryDAO.getAllMediaByGallery(name, isVideo)
            }

        suspend fun getAllMediaByGallery(name: String): List<GalleryMedia>? =
            withIOContext {
                signedGalleryDAO.getAllMediaByGallery(name)
            }

        suspend fun getSignedMedia(uri: Uri): GalleryMedia? = withIOContext {
            signedGalleryDAO.getSignedMedia(uri)
        }

        suspend fun getSignedMedia(path: String): GalleryMedia? = withIOContext {
            signedGalleryDAO.getSignedMedia(path)
        }

        suspend fun deleteSignedMediaByUri(uri: Uri) = withIOContext {
            getSignedMedia(uri)?.let { sendEvent(MediaAction.OnMediaDeleted(it)) }
            signedGalleryDAO.deleteSignedMediaByUri(uri)
        }

        suspend fun deleteSignedMediasByGallery(gallery:String) = withIOContext {
            sendEvent(MediaAction.OnGalleryDeleted(gallery))
            signedGalleryDAO.deleteSignedMediasByGallery(gallery)
        }

        suspend fun deleteSignedMedia(media: GalleryMedia) = withIOContext {
            sendEvent(MediaAction.OnMediaDeleted(media))
            signedGalleryDAO.deleteSignedMedia(media)
        }

        suspend fun insertSignedMedia(media: GalleryMedia): Long = withIOContext {
            sendEvent(MediaAction.OnMediaInserted(media))
            signedGalleryDAO.insertSignedMedia(media)
        }

        suspend fun updateSignedMedia(media: GalleryMedia) = withIOContext {
            sendEvent(MediaAction.OnMediaUpdated(media))
            signedGalleryDAO.updateSignedMedia(media)
        }
    }

    sealed class BaseNoteDAO : BaseDao() {

        private val noteDAO = getNoteDAO()

        suspend fun getAllNote(): List<Note>? = withIOContext {
            noteDAO.getAllNote()
        }

        suspend fun getNoteByName(name: String): Note? = withIOContext {
            noteDAO.getNoteByName(name)
        }

        suspend fun updateNote(note: Note): Int? = withIOContext {
            sendEvent(MediaAction.OnNoteUpdated(note))
            noteDAO.updateNote(note)
        }

        suspend fun deleteNote(note: Note) = withIOContext {
            sendEvent(MediaAction.OnNoteDeleted(note))
            noteDAO.deleteNote(note)
        }

        suspend fun insertNote(note: Note): Long = withIOContext {
            sendEvent(MediaAction.OnNoteInserted(note))
            noteDAO.insertNote(note)
        }

    }

    sealed class BaseNoteTypeDAO : BaseDao() {

        private val noteTypeDAO = getNoteTypeDAO()

        suspend fun getNoteDir(name: String): NoteDir? =
            withIOContext { noteTypeDAO.getNoteDir(name) }

        suspend fun getALLNoteDir(): List<NoteDir>? =
            withIOContext { noteTypeDAO.getALLNoteDir() }

        suspend fun insertNoteDir(noteDir: NoteDir) = withIOContext {
            sendEvent(MediaAction.OnNoteDirInserted(noteDir))
            noteTypeDAO.insertNoteDir(noteDir)
        }

        suspend fun deleteNoteDir(noteDir: NoteDir) = withIOContext {
            sendEvent(MediaAction.OnNoteDirDeleted(noteDir))
            noteTypeDAO.deleteNoteDir(noteDir)
        }
    }

    sealed class BaseGalleryBucketDAO : BaseDao() {

        private val galleryBucketDAO = getGalleryBucketDAO()

        suspend fun getGalleryBucket(name: String): GalleryBucket? = withIOContext {
            galleryBucketDAO.getGalleryBucket(name)
        }

        suspend fun getAllGalleryBucket(isVideo: Boolean): List<GalleryBucket>? =
            withIOContext {
                galleryBucketDAO.getAllGalleryBucket(isVideo)
            }

        suspend fun insertGalleryBucket(galleryBucket: GalleryBucket) = withIOContext {
            sendEvent(MediaAction.OnGalleryBucketInserted(galleryBucket))
            galleryBucketDAO.insertGalleryBucket(galleryBucket)
        }

        suspend fun deleteGalleryBucket(galleryBucket: GalleryBucket) = withIOContext {
            sendEvent(MediaAction.OnGalleryBucketDeleted(galleryBucket))
            galleryBucketDAO.deleteGalleryBucket(galleryBucket)
        }
    }

    sealed class BaseGalleriesWithNotesDAO : BaseDao() {

        private val galleriesWithNotesDAO = getGalleriesWithNotesDAO()

        suspend fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes) =
            withIOContext {
                sendEvent(MediaAction.OnGalleriesWithNotesInserted(galleriesWithNotes))
                galleriesWithNotesDAO.insertGalleriesWithNotes(galleriesWithNotes)
            }

        suspend fun getNotesWithGallery(uri: Uri): List<Note> = withIOContext {
            galleriesWithNotesDAO.getNotesWithGallery(uri) ?: mutableListOf()
        }

        suspend fun getGalleriesWithNote(noteId: Long): List<GalleryMedia> =
            withIOContext {
                galleriesWithNotesDAO.getGalleriesWithNote(noteId) ?: mutableListOf()
            }
    }

    sealed class BaseNoteDirWithNoteDAO : BaseDao() {

        private val noteDirWithNoteDAO = getNoteDirWithNoteDAO()

        suspend fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes) =
            withIOContext {
                sendEvent(MediaAction.OnNoteDirWithNoteInserted(noteDirWithNotes))
                noteDirWithNoteDAO.insertNoteDirWithNote(noteDirWithNotes)
            }

        suspend fun getNotesWithNoteDir(noteDirId: Long): List<Note> = withIOContext {
            noteDirWithNoteDAO.getNotesWithNoteDir(noteDirId) ?: mutableListOf()
        }

        suspend fun getNoteDirWithNote(noteId: Long): List<NoteDir> = withIOContext {
            noteDirWithNoteDAO.getNoteDirWithNote(noteId) ?: mutableListOf()
        }
    }

    sealed class BaseMusicWithMusicBucketDAO : BaseDao() {

        private val musicWithMusicBucketDAO = getMusicWithMusicBucketDAO()

        suspend fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long? =
            withIOContext {
                sendEvent(MediaAction.OnMusicWithMusicBucketInserted(musicWithMusicBucket))
                musicWithMusicBucketDAO.insertMusicWithMusicBucket(musicWithMusicBucket)
            }

        suspend fun deleteMusicWithMusicBucket(musicID: Long, musicBucketId: Long) =
            withIOContext {
                sendEvent(MediaAction.OnMusicWithMusicBucketDeleted(musicID))
                musicWithMusicBucketDAO.deleteMusicWithMusicBucket(musicID, musicBucketId)
            }

        suspend fun getMusicWithMusicBucket(musicBucketId: Long): List<Music> =
            withIOContext {
                musicWithMusicBucketDAO.getMusicWithMusicBucket(musicBucketId) ?: mutableListOf()
            }

        suspend fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket> =
            withIOContext {
                musicWithMusicBucketDAO.getMusicBucketWithMusic(musicID) ?: mutableListOf()
            }
    }

    abstract class BaseDao {
        protected abstract fun sendEvent(mediaAction: MediaAction)
    }

}