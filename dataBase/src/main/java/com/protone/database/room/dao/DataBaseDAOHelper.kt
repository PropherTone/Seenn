package com.protone.database.room.dao

import android.net.Uri
import com.protone.database.room.*
import com.protone.database.room.entity.*

object DataBaseDAOHelper : BaseDAOHelper(), MusicBucketDAO, MusicDAO, SignedGalleyDAO, NoteDAO,
    NoteTypeDAO, GalleyBucketDAO {

    //MusicBucket
    private var musicBucketDAO: MusicBucketDAO? = null

    init {
        musicBucketDAO()
    }

    private fun musicBucketDAO() {
        if (musicBucketDAO == null) {
            musicBucketDAO = getMusicBucketDAO()
        }
    }

    inline fun getAllMusicBucket(crossinline callBack: (result: List<MusicBucket>?) -> Unit) {
        execute {
            getAllMusicBucket()?.let {
                callBack(it)
            }
        }
    }

    override fun getAllMusicBucket(): List<MusicBucket>? {
        return musicBucketDAO?.getAllMusicBucket()
    }

    override fun getMusicBucketByName(name: String): MusicBucket? {
        return musicBucketDAO?.getMusicBucketByName(name)
    }

    override fun addMusicBucket(musicBucket: MusicBucket) {
        musicBucketDAO?.addMusicBucket(musicBucket)
    }

    fun addMusicBucketThread(musicBucket: MusicBucket) {
        execute {
            musicBucketDAO?.addMusicBucket(musicBucket)
        }
    }

    inline fun addMusicBucketWithCallBack(
        musicBucket: MusicBucket,
        crossinline callBack: (result: Boolean, name: String) -> Unit
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

    override fun updateMusicBucket(bucket: MusicBucket): Int {
        return musicBucketDAO?.updateMusicBucket(bucket) ?: -1
    }

    fun updateMusicBucketBack(bucket: MusicBucket) {
        execute {
            updateMusicBucket(bucket)
        }
    }

    inline fun updateMusicBucketCB(bucket: MusicBucket, crossinline callBack: (Int) -> Unit) {
        execute {
            callBack.invoke(updateMusicBucket(bucket))
        }
    }

    fun deleteMusicBucketCB(bucket: MusicBucket, callBack: (Boolean) -> Unit) {
        execute {
            musicBucketDAO?.deleteMusicBucket(bucket)
            callBack(musicBucketDAO?.getMusicBucketByName(bucket.name) == null)
        }
    }

    override fun deleteMusicBucket(bucket: MusicBucket) {
        execute {
            musicBucketDAO?.deleteMusicBucket(bucket)
        }
    }


    //Music
    private var musicDAO: MusicDAO? = null

    init {
        if (musicDAO == null) {
            musicDAO = getMusicDAO()
        }
    }

    fun insertMusicMulti(music: List<Music>) {
        execute {
            music.forEach {
                musicDAO?.insertMusic(it)
            }
        }
    }

    fun deleteMusicMulti(music: List<Music>) {
        execute {
            music.forEach {
                musicDAO?.deleteMusic(it)
            }
        }
    }


    inline fun getAllMusic(crossinline callBack: (List<Music>) -> Unit) {
        execute {
            getAllMusic()?.let {
                callBack(it)
            }
        }
    }

    fun insertMusicCheck(music: Music) {
        musicDAO?.getMusicByUri(music.uri).let {
            if (it == null) insertMusic(music)
        }
    }

    override fun insertMusic(music: Music) {
        musicDAO?.insertMusic(music)
    }

    override fun getAllMusic(): List<Music>? = musicDAO?.getAllMusic()

    override fun deleteMusic(music: Music) {
        execute {
            musicDAO?.deleteMusic(music)
        }
    }

    override fun updateMusic(music: Music): Int = musicDAO?.updateMusic(music) ?: -1

    override fun updateMusicMyBucket(name: String, bucket: List<String>): Int =
        musicDAO?.updateMusicMyBucket(name, bucket) ?: -1

    override fun getMusicByUri(uri: Uri): Music? = musicDAO?.getMusicByUri(uri)

    inline fun updateMusicCB(music: Music, crossinline callBack: (Int) -> Unit) {
        execute {
            callBack(updateMusic(music))
        }
    }

    inline fun updateMusicMyBucketCB(
        name: String,
        bucket: List<String>,
        crossinline callBack: (Int) -> Unit
    ) {
        execute {
            callBack(updateMusicMyBucket(name, bucket))
        }
    }

    //Galley
    private var signedGalleyDAO: SignedGalleyDAO? = null

    init {
        if (signedGalleyDAO == null) {
            signedGalleyDAO = getGalleyDAO()
        }
    }

    inline fun getAllSignedMedia(crossinline callBack: (List<GalleyMedia>) -> Unit) {
        execute {
            getAllSignedMedia()?.let {
                callBack(it)
            }
        }
    }

    override fun getAllSignedMedia(): List<GalleyMedia>? = signedGalleyDAO?.getAllSignedMedia()
    override fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>? =
        signedGalleyDAO?.getAllMediaByType(isVideo)

    override fun getAllGalley(isVideo: Boolean): List<String>? {
        return signedGalleyDAO?.getAllGalley(isVideo)
    }

    override fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>? {
        return signedGalleyDAO?.getAllMediaByGalley(name, isVideo)
    }

    fun deleteSignedMedias(list: MutableList<GalleyMedia>) {
        execute {
            list.forEach {
                deleteSignedMediaByUri(it.uri)
            }
        }
    }

    override fun deleteSignedMediaByUri(uri: Uri) {
        signedGalleyDAO?.deleteSignedMediaByUri(uri)
    }

    override fun deleteSignedMedia(media: GalleyMedia) {
        execute {
            signedGalleyDAO?.deleteSignedMedia(media)
        }
    }

    fun updateMediaMulti(list: MutableList<GalleyMedia>) {
        execute { list.forEach { updateSignedMedia(it) } }
    }

    override fun insertSignedMedia(media: GalleyMedia) {
        execute { sortSignedMedia(media) }
    }

    fun sortSignedMedia(media: GalleyMedia) {
        getSignedMedia(media.uri).let {
            if (it != null) {
                it.name = media.name
                it.path = media.path
                it.bucket = media.bucket
                it.type = media.type
                it.date = media.date
                updateSignedMedia(it)
            } else signedGalleyDAO?.insertSignedMedia(media)
        }
    }

    override fun getSignedMedia(uri: Uri): GalleyMedia? = signedGalleyDAO?.getSignedMedia(uri)

    override fun getSignedMedia(path: String): GalleyMedia? = signedGalleyDAO?.getSignedMedia(path)

    fun getSignedMediaCB(uri: Uri, callBack: (GalleyMedia?) -> Unit) {
        execute {
            callBack.invoke(getSignedMedia(uri))
        }
    }

    override fun updateSignedMedia(galleyMedia: GalleyMedia) {
        signedGalleyDAO?.updateSignedMedia(galleyMedia)
    }

    //Note
    private var noteDAO: NoteDAO? = null

    init {
        if (noteDAO == null) {
            noteDAO = getNoteDAO()
        }
    }

    override fun getAllNote(): List<Note>? {
        return noteDAO?.getAllNote()
    }

    override fun getNoteByName(name: String): Note? {
        return noteDAO?.getNoteByName(name)
    }

    override fun insertNote(note: Note) {
        noteDAO?.insertNote(note)
    }

    inline fun insertNoteCB(note: Note, crossinline callBack: (Boolean, String) -> Unit) {
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
            insertNote(note)
            callBack.invoke(getNoteByName(note.title) != null, note.title)
        }
    }

    //NoteType
    private var noteTypeDAO: NoteTypeDAO? = null

    init {
        if (noteTypeDAO == null) {
            noteTypeDAO = getNoteTypeDAO()
        }
    }

    inline fun insertNoteTypeCB(
        noteType: NoteType,
        crossinline callBack: (Boolean, String) -> Unit
    ) {
        execute {
            var count = 0
            val tempName = noteType.type
            val names = mutableMapOf<String, Int>()
            getALLNoteType()?.forEach {
                names[it.type] = 1
                if (it.type == noteType.type) {
                    noteType.type = "${tempName}(${++count})"
                }
            }
            while (names[noteType.type] != null) {
                noteType.type = "${tempName}(${++count})"
            }
            insertNoteType(noteType)
            callBack.invoke(getNoteType(noteType.type) != null, noteType.type)
        }
    }

    override fun insertNoteType(noteType: NoteType) {
        noteTypeDAO?.insertNoteType(noteType)
    }

    override fun getNoteType(name: String): NoteType? = noteTypeDAO?.getNoteType(name)


    override fun deleteNoteType(noteType: NoteType) {
        noteTypeDAO?.deleteNoteType(noteType)
    }

    fun doDeleteNoteType(noteType: NoteType, callBack: (Boolean) -> Unit) {
        execute {
            deleteNoteType(noteType)
            callBack.invoke(getNoteType(noteType.type) != null)
        }
    }

    override fun getALLNoteType(): List<NoteType>? = noteTypeDAO?.getALLNoteType()

    //GalleyBucket
    private var galleyBucketDAO: GalleyBucketDAO? = null

    init {
        if (galleyBucketDAO == null) {
            galleyBucketDAO = getGalleyBucketDAO()
        }
    }

    inline fun insertGalleyBucketCB(
        galleyBucket: GalleyBucket,
        crossinline callBack: (Boolean, String) -> Unit
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

    override fun insertGalleyBucket(galleyBucket: GalleyBucket) {
        galleyBucketDAO?.insertGalleyBucket(galleyBucket)
    }

    override fun getGalleyBucket(name: String): GalleyBucket? =
        galleyBucketDAO?.getGalleyBucket(name)

    override fun deleteGalleyBucket(galleyBucket: GalleyBucket) {
        execute {
            galleyBucketDAO?.deleteGalleyBucket(galleyBucket)
        }
    }


    fun getGalleyBucket(name: String, callBack: (GalleyBucket?) -> Unit) {
        execute {
            callBack.invoke(getGalleyBucket(name))
        }
    }

    override fun getALLGalleyBucket(isVideo: Boolean): List<GalleyBucket>? =
        galleyBucketDAO?.getALLGalleyBucket(isVideo)

}

