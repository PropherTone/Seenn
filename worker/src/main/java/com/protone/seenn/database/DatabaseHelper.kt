package com.protone.seenn.database

import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.entity.*
import com.protone.api.isInDebug
import com.protone.api.onResult
import com.protone.database.R
import com.protone.database.room.*
import kotlinx.coroutines.*

class DatabaseHelper {

    val executorService by lazy { CoroutineScope(Dispatchers.IO) }

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

    private val musicBucketDAO = getMusicBucketDAO()
    private val musicDAO = getMusicDAO()
    private val signedGalleyDAO = getGalleyDAO()
    private val noteDAO = getNoteDAO()
    private val noteTypeDAO = getNoteTypeDAO()
    private val galleyBucketDAO = getGalleyBucketDAO()
    private val galleriesWithNotesDAO = getGalleriesWithNotesDAO()
    private val noteDirWithNoteDAO = getNoteDirWithNoteDAO()
    private val musicWithMusicBucketDAO = getMusicWithMusicBucketDAO()

    val musicBucketDAOBridge by lazy { MusicBucketDAOBridge() }
    val musicDAOBridge by lazy { MusicDAOBridge() }
    val signedGalleyDAOBridge by lazy { GalleyDAOBridge() }
    val noteDAOBridge by lazy { NoteDAOBridge() }
    val noteDirDAOBridge by lazy { NoteTypeDAOBridge() }
    val galleyBucketDAOBridge by lazy { GalleyBucketDAOBridge() }
    val galleriesWithNotesDAOBridge by lazy { GalleriesWithNotesDAOBridge() }
    val noteDirWithNoteDAOBridge by lazy { NoteDirWithNoteDAOBridge() }
    val musicWithMusicBucketDAOBridge by lazy { MusicWithMusicBucketDAOBridge() }

    inline fun execute(crossinline runnable: suspend () -> Unit) {
        executorService.launch(Dispatchers.IO) {
            try {
                runnable.invoke()
            } catch (e: Exception) {
                if (isInDebug()) e.printStackTrace()
                R.string.unknown_error.getString().toast()
            } finally {
                cancel()
            }
        }
    }

    fun shutdownNow() {
        if (executorService.isActive) {
            executorService.cancel()
        }
    }

    inner class MusicBucketDAOBridge {
        suspend fun getAllMusicBucketRs() = onResult {
            execute {
                it.resumeWith(Result.success(getAllMusicBucket()))
            }
        }

        fun getAllMusicBucket(): List<MusicBucket>? {
            return musicBucketDAO.getAllMusicBucket()
        }

        fun getMusicBucketByName(name: String): MusicBucket? {
            return musicBucketDAO.getMusicBucketByName(name)
        }

        fun addMusicBucket(musicBucket: MusicBucket) {
            musicBucketDAO.addMusicBucket(musicBucket)
        }

        fun addMusicBucketAsync(musicBucket: MusicBucket) {
            execute {
                musicBucketDAO.addMusicBucket(musicBucket)
            }
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

        fun updateMusicBucket(bucket: MusicBucket): Int {
            return musicBucketDAO.updateMusicBucket(bucket)
        }

        fun updateMusicBucketAsync(bucket: MusicBucket) {
            execute {
                updateMusicBucket(bucket)
            }
        }

        suspend fun updateMusicBucketRs(bucket: MusicBucket) = onResult {
            execute {
                it.resumeWith(Result.success(updateMusicBucket(bucket)))
            }
        }

        suspend fun deleteMusicBucketRs(bucket: MusicBucket) = onResult {
            execute {
                deleteMusicBucket(bucket)
                it.resumeWith(Result.success(musicBucketDAO.getMusicBucketByName(bucket.name) == null))
            }
        }

        fun deleteMusicBucketAsync(bucket: MusicBucket) {
            execute {
                deleteMusicBucket(bucket)
            }
        }

        fun deleteMusicBucket(bucket: MusicBucket) {
            musicBucketDAO.deleteMusicBucket(bucket)
        }
    }

    inner class MusicDAOBridge {
        fun insertMusicMultiAsync(music: List<Music>) {
            execute {
                music.forEach {
                    musicDAO.insertMusic(it)
                }
            }
        }

        fun deleteMusicMultiAsync(music: List<Music>) {
            execute {
                music.forEach {
                    deleteMusic(it)
                }
            }
        }


        suspend fun getAllMusicRs() = onResult {
            execute {
                it.resumeWith(Result.success(getAllMusic()))
            }
        }

        fun insertMusicCheck(music: Music) {
            musicDAO.getMusicByUri(music.uri).let {
                if (it == null) insertMusic(music)
            }
        }

        fun insertMusic(music: Music) {
            musicDAO.insertMusic(music)
        }

        fun getAllMusic(): List<Music>? = musicDAO.getAllMusic()

        fun deleteMusicAsync(music: Music) {
            execute {
                deleteMusic(music)
            }
        }

        fun deleteMusic(music: Music) {
            musicDAO.deleteMusic(music)
        }

        fun updateMusic(music: Music): Int = musicDAO.updateMusic(music)

        fun getMusicByUri(uri: Uri): Music? = musicDAO.getMusicByUri(uri)

        suspend fun updateMusicRs(music: Music) = onResult {
            execute {
                it.resumeWith(Result.success(updateMusic(music)))
            }
        }
    }

    inner class GalleyDAOBridge {
        suspend fun getAllSignedMediaRs() = onResult {
            execute {
                it.resumeWith(Result.success(getAllSignedMedia()))
            }
        }

        fun getAllSignedMedia(): List<GalleyMedia>? =
            signedGalleyDAO.getAllSignedMedia()

        fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>? =
            signedGalleyDAO.getAllMediaByType(isVideo)

        fun getAllGalley(isVideo: Boolean): List<String>? {
            return signedGalleyDAO.getAllGalley(isVideo)
        }

        fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>? {
            return signedGalleyDAO.getAllMediaByGalley(name, isVideo)
        }

        fun deleteSignedMediaMultiAsync(list: MutableList<GalleyMedia>) {
            execute {
                list.forEach {
                    deleteSignedMediaByUri(it.uri)
                }
            }
        }

        fun deleteSignedMediaByUri(uri: Uri) {
            signedGalleyDAO.deleteSignedMediaByUri(uri)
        }

        fun deleteSignedMediaAsync(media: GalleyMedia) {
            execute {
                deleteSignedMedia(media)
            }
        }

        fun deleteSignedMedia(media: GalleyMedia) {
            signedGalleyDAO.deleteSignedMedia(media)
        }

        fun updateMediaMultiAsync(list: MutableList<GalleyMedia>) {
            execute { list.forEach { updateSignedMedia(it) } }
        }

        fun insertSignedMedia(media: GalleyMedia) {
            signedGalleyDAO.insertSignedMedia(media)
        }

        fun insertSignedMediaChecked(media: GalleyMedia): GalleyMedia? {
            val it = getSignedMedia(media.uri)
            return if (it != null) {
                if (it == media) return null
                it.name = media.name
                it.path = media.path
                it.bucket = media.bucket
                it.type = media.type
                it.date = media.date
                updateSignedMedia(it)
                it
            } else {
                insertSignedMedia(media)
                media
            }
        }

        fun getSignedMedia(uri: Uri): GalleyMedia? =
            signedGalleyDAO.getSignedMedia(uri)

        fun getSignedMedia(path: String): GalleyMedia? =
            signedGalleyDAO.getSignedMedia(path)

        suspend fun getSignedMediaRs(uri: Uri) = onResult {
            execute {
                it.resumeWith(Result.success(getSignedMedia(uri)))
            }
        }

        fun updateSignedMedia(galleyMedia: GalleyMedia) {
            signedGalleyDAO.updateSignedMedia(galleyMedia)
        }
    }

    inner class NoteDAOBridge {
        fun getAllNote(): List<Note>? {
            return noteDAO.getAllNote()
        }

        fun getNoteByName(name: String): Note? {
            return noteDAO.getNoteByName(name)
        }

        fun updateNote(note: Note): Int? {
            return noteDAO.updateNote(note)
        }

        fun deleteNoteAsync(note: Note) {
            execute {
                noteDAO.deleteNote(note)
            }
        }

        fun deleteNote(note: Note) {
            noteDAO.deleteNote(note)
        }

        fun insertNote(note: Note): Long {
            return noteDAO.insertNote(note)
        }

        suspend fun insertNoteRs(note: Note) =
            onResult {
                execute {
                    var count = 0
                    val tempName = note.title
                    val names = mutableMapOf<String, Int>()
                    getAllNote()?.forEach {
                        names[it.title] = 1
                        if (it.title == note.title) {
                            note.title = "${tempName}(${++count})"
                        }
                    }
                    while (names[note.title] != null) {
                        note.title = "${tempName}(${++count})"
                    }
                    val id = insertNote(note)
                    it.resumeWith(Result.success(Pair(getNoteByName(note.title) != null, id)))
                }
            }
    }

    inner class NoteTypeDAOBridge {
        suspend fun insertNoteDirRs(
            noteDir: NoteDir,
        ) = onResult<Pair<Boolean, String>> {
            execute {
                var count = 0
                val tempName = noteDir.name
                val names = mutableMapOf<String, Int>()
                getALLNoteDir()?.forEach {
                    names[it.name] = 1
                    if (it.name == noteDir.name) {
                        noteDir.name = "${tempName}(${++count})"
                    }
                }
                while (names[noteDir.name] != null) {
                    noteDir.name = "${tempName}(${++count})"
                }
                insertNoteDir(noteDir)
                it.resumeWith(Result.success(Pair(getNoteDir(noteDir.name) != null, noteDir.name)))
            }
        }

        fun insertNoteDir(noteDir: NoteDir) {
            noteTypeDAO.insertNoteDir(noteDir)
        }

        fun getNoteDir(name: String): NoteDir? = noteTypeDAO.getNoteDir(name)


        fun deleteNoteDir(noteDir: NoteDir) {
            noteTypeDAO.deleteNoteDir(noteDir)
        }

        suspend fun doDeleteNoteDirRs(noteDir: NoteDir) = onResult {
            execute {
                deleteNoteDir(noteDir)
                it.resumeWith(Result.success(getNoteDir(noteDir.name) != null))
            }
        }

        fun getALLNoteDir(): List<NoteDir>? = noteTypeDAO.getALLNoteDir()
    }

    inner class GalleyBucketDAOBridge {
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

        fun insertGalleyBucket(galleyBucket: GalleyBucket) {
            galleyBucketDAO.insertGalleyBucket(galleyBucket)
        }

        fun getGalleyBucket(name: String): GalleyBucket? =
            galleyBucketDAO.getGalleyBucket(name)

        fun deleteGalleyBucketAsync(galleyBucket: GalleyBucket) {
            execute {
                deleteGalleyBucket(galleyBucket)
            }
        }

        fun deleteGalleyBucket(galleyBucket: GalleyBucket) {
            galleyBucketDAO.deleteGalleyBucket(galleyBucket)
        }

        suspend fun getGalleyBucketRs(name: String) = onResult {
            execute {
                it.resumeWith(Result.success(getGalleyBucket(name)))
            }
        }

        fun getALLGalleyBucket(isVideo: Boolean): List<GalleyBucket>? =
            galleyBucketDAO.getALLGalleyBucket(isVideo)
    }

    inner class GalleriesWithNotesDAOBridge {
        fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes) {
            galleriesWithNotesDAO.insertGalleriesWithNotes(galleriesWithNotes)
        }

        fun getNotesWithGalley(mediaId: Long): List<Note> {
            return galleriesWithNotesDAO.getNotesWithGalley(mediaId) ?: mutableListOf()
        }

        fun getGalleriesWithNote(noteId: Long): List<GalleyMedia> {
            return galleriesWithNotesDAO.getGalleriesWithNote(noteId) ?: mutableListOf()
        }
    }

    inner class NoteDirWithNoteDAOBridge {
        fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes) {
            noteDirWithNoteDAO.insertNoteDirWithNote(noteDirWithNotes)
        }

        fun getNotesWithNoteDir(noteDirId: Long): List<Note> {
            return noteDirWithNoteDAO.getNotesWithNoteDir(noteDirId) ?: mutableListOf()
        }

        fun getNoteDirWithNote(noteId: Long): List<NoteDir> {
            return noteDirWithNoteDAO.getNoteDirWithNote(noteId) ?: mutableListOf()
        }
    }

    inner class MusicWithMusicBucketDAOBridge {
        suspend fun insertMusicWithMusicBucket(musicID: Long, bucket: String) =
            onResult { co ->
                val musicBucket = musicBucketDAOBridge.getMusicBucketByName(bucket)
                if (musicBucket == null) {
                    co.resumeWith(Result.success(-1L))
                    return@onResult
                }
                val entity = MusicWithMusicBucket(
                    musicBucket.musicBucketId,
                    musicID
                )
                val success = Result.success(insertMusicWithMusicBucket(entity) ?: -1)
                co.resumeWith(success)
            }

        fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long? {
            return musicWithMusicBucketDAO.insertMusicWithMusicBucket(musicWithMusicBucket)
        }

        fun deleteMusicWithMusicBucketAsync(musicID: Long) {
            execute {
                deleteMusicWithMusicBucket(musicID)
            }
        }

        fun deleteMusicWithMusicBucket(musicID: Long) {
            musicWithMusicBucketDAO.deleteMusicWithMusicBucket(musicID)
        }

        fun getMusicWithMusicBucket(musicBucketId: Long): List<Music> {
            return musicWithMusicBucketDAO.getMusicWithMusicBucket(musicBucketId) ?: mutableListOf()
        }

        fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket> {
            return musicWithMusicBucketDAO.getMusicBucketWithMusic(musicID) ?: mutableListOf()
        }
    }
}