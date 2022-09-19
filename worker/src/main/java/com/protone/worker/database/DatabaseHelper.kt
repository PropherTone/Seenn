package com.protone.worker.database

import android.content.Context
import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
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
    val signedGalleyDAOBridge by lazy { GalleyDAOBridge() }
    val noteDAOBridge by lazy { NoteDAOBridge() }
    val noteDirDAOBridge by lazy { NoteTypeDAOBridge() }
    val galleyBucketDAOBridge by lazy { GalleyBucketDAOBridge() }
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

    inner class GalleyDAOBridge : BaseGalleyDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun deleteSignedMediaMultiAsync(list: MutableList<GalleyMedia>) {
            execute {
                list.forEach {
                    deleteSignedMediaByUri(it.uri)
                }
            }
        }

        fun deleteSignedMediaAsync(media: GalleyMedia) {
            execute {
                deleteSignedMedia(media)
            }
        }

        fun updateMediaMultiAsync(list: MutableList<GalleyMedia>) {
            execute { list.forEach { updateSignedMedia(it) } }
        }

        suspend fun insertSignedMediaChecked(media: GalleyMedia): GalleyMedia? {
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

        suspend fun getAllSignedMediaRs(): List<GalleyMedia>? {
            return getAllSignedMedia()
        }

        suspend fun getAllMediaByTypeRs(isVideo: Boolean): List<GalleyMedia>? {
            return getAllMediaByType(isVideo)
        }

        suspend fun getSignedMediaRs(uri: Uri): GalleyMedia? {
            return getSignedMedia(uri)
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

    inner class GalleyBucketDAOBridge : BaseGalleyBucketDAO() {

        override fun sendEvent(mediaAction: MediaAction) {
            pollEvent(mediaAction)
        }

        fun deleteGalleyBucketAsync(galleyBucket: GalleyBucket) {
            execute {
                deleteGalleyBucket(galleyBucket)
            }
        }

        inline fun insertGalleyBucketCB(
            galleyBucket: GalleyBucket,
            crossinline callBack: suspend (Boolean, String) -> Unit
        ) {
            execute {
                var count = 0
                val tempName = galleyBucket.type
                val names = mutableMapOf<String, Int>()
                getALLGalleyBucket(galleyBucket.isImage)?.forEach {
                    names[it.type] = 1
                    if (it.type == galleyBucket.type) {
                        galleyBucket.type = "${tempName}(${++count})"
                    }
                }
                while (names[galleyBucket.type] != null) {
                    galleyBucket.type = "${tempName}(${++count})"
                }
                insertGalleyBucket(galleyBucket)
                callBack.invoke(getGalleyBucket(galleyBucket.type) != null, galleyBucket.type)
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

        suspend fun getAllMusicBucket(): List<MusicBucket>? = withContext(Dispatchers.IO) {
            musicBucketDAO.getAllMusicBucket()
        }

        suspend fun getMusicBucketByName(name: String): MusicBucket? = withContext(Dispatchers.IO) {
            musicBucketDAO.getMusicBucketByName(name)
        }

        suspend fun addMusicBucket(musicBucket: MusicBucket) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnNewMusicBucket(musicBucket))
            musicBucketDAO.addMusicBucket(musicBucket)
        }

        suspend fun updateMusicBucket(musicBucket: MusicBucket): Int = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMusicBucketUpdated(musicBucket))
            musicBucketDAO.updateMusicBucket(musicBucket)
        }

        suspend fun deleteMusicBucket(musicBucket: MusicBucket) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMusicBucketDeleted(musicBucket))
            musicBucketDAO.deleteMusicBucket(musicBucket)
        }

    }

    sealed class BaseMusicDAO : BaseDao() {

        private val musicDAO = getMusicDAO()

        suspend fun getAllMusic(): List<Music>? =
            withContext(Dispatchers.IO) { musicDAO.getAllMusic() }

        suspend fun getMusicByUri(uri: Uri): Music? =
            withContext(Dispatchers.IO) { musicDAO.getMusicByUri(uri) }

        suspend fun insertMusic(music: Music) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMusicInserted(music))
            musicDAO.insertMusic(music)
        }

        suspend fun deleteMusic(music: Music) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMusicDeleted(music))
            musicDAO.deleteMusic(music)
        }

        suspend fun updateMusic(music: Music): Int = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMusicUpdate(music))
            musicDAO.updateMusic(music)
        }

    }

    sealed class BaseGalleyDAO : BaseDao() {

        private val signedGalleyDAO = getGalleyDAO()

        suspend fun getAllSignedMedia(): List<GalleyMedia>? = withContext(Dispatchers.IO) {
            signedGalleyDAO.getAllSignedMedia()
        }

        suspend fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>? =
            withContext(Dispatchers.IO) {
                signedGalleyDAO.getAllMediaByType(isVideo)
            }

        suspend fun getAllGalley(isVideo: Boolean): List<String>? = withContext(Dispatchers.IO) {
            signedGalleyDAO.getAllGalley(isVideo)
        }

        suspend fun getAllGalley(): List<String>? = withContext(Dispatchers.IO) {
            signedGalleyDAO.getAllGalley()
        }

        suspend fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>? =
            withContext(Dispatchers.IO) {
                signedGalleyDAO.getAllMediaByGalley(name, isVideo)
            }

        suspend fun getAllMediaByGalley(name: String): List<GalleyMedia>? =
            withContext(Dispatchers.IO) {
                signedGalleyDAO.getAllMediaByGalley(name)
            }

        suspend fun getSignedMedia(uri: Uri): GalleyMedia? = withContext(Dispatchers.IO) {
            signedGalleyDAO.getSignedMedia(uri)
        }

        suspend fun getSignedMedia(path: String): GalleyMedia? = withContext(Dispatchers.IO) {
            signedGalleyDAO.getSignedMedia(path)
        }

        suspend fun deleteSignedMediaByUri(uri: Uri) = withContext(Dispatchers.IO) {
            getSignedMedia(uri)?.let { sendEvent(MediaAction.OnMediaByUriDeleted(it)) }
            signedGalleyDAO.deleteSignedMediaByUri(uri)
        }

        suspend fun deleteSignedMedia(media: GalleyMedia) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMediaDeleted(media))
            signedGalleyDAO.deleteSignedMedia(media)
        }

        suspend fun insertSignedMedia(media: GalleyMedia): Long = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMediaInserted(media))
            signedGalleyDAO.insertSignedMedia(media)
        }

        suspend fun updateSignedMedia(media: GalleyMedia) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnMediaUpdated(media))
            signedGalleyDAO.updateSignedMedia(media)
        }
    }

    sealed class BaseNoteDAO : BaseDao() {

        private val noteDAO = getNoteDAO()

        suspend fun getAllNote(): List<Note>? = withContext(Dispatchers.IO) {
            noteDAO.getAllNote()
        }

        suspend fun getNoteByName(name: String): Note? = withContext(Dispatchers.IO) {
            noteDAO.getNoteByName(name)
        }

        suspend fun updateNote(note: Note): Int? = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnNoteUpdated(note))
            noteDAO.updateNote(note)
        }

        suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnNoteDeleted(note))
            noteDAO.deleteNote(note)
        }

        suspend fun insertNote(note: Note): Long = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnNoteInserted(note))
            noteDAO.insertNote(note)
        }

    }

    sealed class BaseNoteTypeDAO : BaseDao() {

        private val noteTypeDAO = getNoteTypeDAO()

        suspend fun getNoteDir(name: String): NoteDir? =
            withContext(Dispatchers.IO) { noteTypeDAO.getNoteDir(name) }

        suspend fun getALLNoteDir(): List<NoteDir>? =
            withContext(Dispatchers.IO) { noteTypeDAO.getALLNoteDir() }

        suspend fun insertNoteDir(noteDir: NoteDir) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnNoteDirInserted(noteDir))
            noteTypeDAO.insertNoteDir(noteDir)
        }

        suspend fun deleteNoteDir(noteDir: NoteDir) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnNoteDirDeleted(noteDir))
            noteTypeDAO.deleteNoteDir(noteDir)
        }
    }

    sealed class BaseGalleyBucketDAO : BaseDao() {

        private val galleyBucketDAO = getGalleyBucketDAO()

        suspend fun getGalleyBucket(name: String): GalleyBucket? = withContext(Dispatchers.IO) {
            galleyBucketDAO.getGalleyBucket(name)
        }

        suspend fun getALLGalleyBucket(isVideo: Boolean): List<GalleyBucket>? =
            withContext(Dispatchers.IO) {
                galleyBucketDAO.getALLGalleyBucket(isVideo)
            }

        suspend fun insertGalleyBucket(galleyBucket: GalleyBucket) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnGalleyBucketInserted(galleyBucket))
            galleyBucketDAO.insertGalleyBucket(galleyBucket)
        }

        suspend fun deleteGalleyBucket(galleyBucket: GalleyBucket) = withContext(Dispatchers.IO) {
            sendEvent(MediaAction.OnGalleyBucketDeleted(galleyBucket))
            galleyBucketDAO.deleteGalleyBucket(galleyBucket)
        }
    }

    sealed class BaseGalleriesWithNotesDAO : BaseDao() {

        private val galleriesWithNotesDAO = getGalleriesWithNotesDAO()

        suspend fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes) =
            withContext(Dispatchers.IO) {
                sendEvent(MediaAction.OnGalleriesWithNotesInserted(galleriesWithNotes))
                galleriesWithNotesDAO.insertGalleriesWithNotes(galleriesWithNotes)
            }

        suspend fun getNotesWithGalley(uri: Uri): List<Note> = withContext(Dispatchers.IO) {
            galleriesWithNotesDAO.getNotesWithGalley(uri) ?: mutableListOf()
        }

        suspend fun getGalleriesWithNote(noteId: Long): List<GalleyMedia> =
            withContext(Dispatchers.IO) {
                galleriesWithNotesDAO.getGalleriesWithNote(noteId) ?: mutableListOf()
            }
    }

    sealed class BaseNoteDirWithNoteDAO : BaseDao() {

        private val noteDirWithNoteDAO = getNoteDirWithNoteDAO()

        suspend fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes) =
            withContext(Dispatchers.IO) {
                sendEvent(MediaAction.OnNoteDirWithNoteInserted(noteDirWithNotes))
                noteDirWithNoteDAO.insertNoteDirWithNote(noteDirWithNotes)
            }

        suspend fun getNotesWithNoteDir(noteDirId: Long): List<Note> = withContext(Dispatchers.IO) {
            noteDirWithNoteDAO.getNotesWithNoteDir(noteDirId) ?: mutableListOf()
        }

        suspend fun getNoteDirWithNote(noteId: Long): List<NoteDir> = withContext(Dispatchers.IO) {
            noteDirWithNoteDAO.getNoteDirWithNote(noteId) ?: mutableListOf()
        }
    }

    sealed class BaseMusicWithMusicBucketDAO : BaseDao() {

        private val musicWithMusicBucketDAO = getMusicWithMusicBucketDAO()

        suspend fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long? =
            withContext(Dispatchers.IO) {
                sendEvent(MediaAction.OnMusicWithMusicBucketInserted(musicWithMusicBucket))
                musicWithMusicBucketDAO.insertMusicWithMusicBucket(musicWithMusicBucket)
            }

        suspend fun deleteMusicWithMusicBucket(musicID: Long, musicBucketId: Long) =
            withContext(Dispatchers.IO) {
                sendEvent(MediaAction.OnMusicWithMusicBucketDeleted(musicID))
                musicWithMusicBucketDAO.deleteMusicWithMusicBucket(musicID, musicBucketId)
            }

        suspend fun getMusicWithMusicBucket(musicBucketId: Long): List<Music> =
            withContext(Dispatchers.IO) {
                musicWithMusicBucketDAO.getMusicWithMusicBucket(musicBucketId) ?: mutableListOf()
            }

        suspend fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket> =
            withContext(Dispatchers.IO) {
                musicWithMusicBucketDAO.getMusicBucketWithMusic(musicID) ?: mutableListOf()
            }
    }

    abstract class BaseDao {
        protected abstract fun sendEvent(mediaAction: MediaAction)
    }

}