package com.protone.worker.database

import android.net.Uri
import com.protone.api.entity.*

sealed class MediaAction {

    data class OnNewMusicBucket(val musicBucket: MusicBucket) : MediaAction()
    data class OnMusicBucketUpdated(val musicBucket: MusicBucket) : MediaAction()
    data class OnMusicBucketDeleted(val musicBucket: MusicBucket) : MediaAction()

    data class OnMusicInserted(val music: Music) : MediaAction()
    data class OnMusicDeleted(val music: Music) : MediaAction()
    data class OnMusicUpdate(val music: Music) : MediaAction()

    data class OnSignedMediaByUriDeleted(val uri: Uri) : MediaAction()
    data class OnSignedMediaDeleted(val media: GalleyMedia) : MediaAction()
    data class OnSignedMediaInserted(val media: GalleyMedia) : MediaAction()
    data class OnSignedMediaUpdated(val media: GalleyMedia) : MediaAction()

    data class OnNoteUpdated(val note: Note) : MediaAction()
    data class OnNoteDeleted(val note: Note) : MediaAction()
    data class OnNoteInserted(val note: Note) : MediaAction()

    data class OnNoteDirInserted(val noteDir: NoteDir) : MediaAction()
    data class OnNoteDirDeleted(val noteDir: NoteDir) : MediaAction()

    data class OnGalleyBucketInserted(val galleyBucket: GalleyBucket) : MediaAction()
    data class OnGalleyBucketDeleted(val galleyBucket: GalleyBucket) : MediaAction()

    data class OnGalleriesWithNotesInserted(val galleriesWithNotes: GalleriesWithNotes) :
        MediaAction()

    data class OnNoteDirWithNoteInserted(val noteDirWithNotes: NoteDirWithNotes) :
        MediaAction()

    data class OnMusicWithMusicBucketInserted(val musicWithMusicBucket: MusicWithMusicBucket) :
        MediaAction()
    data class OnMusicWithMusicBucketDeleted(val musicID: Long) : MediaAction()
}

//sealed class MusicBucketAction {
//
//}
//
//sealed class MusicAction {
//
//}
//
//sealed class GalleyAction {
//
//}
//
//sealed class NoteAction {
//
//}
//
//sealed class NoteTypeAction {
//
//}
//
//sealed class GalleyBucketAction {
//
//}
//
//sealed class GalleriesWithNotesAction {
//
//}
//
//sealed class NoteDirWithNoteAction {
//
//}
//
//sealed class MusicWithMusicBucketAction {
//
//}
