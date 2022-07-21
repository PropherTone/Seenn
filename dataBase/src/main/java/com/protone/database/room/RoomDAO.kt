package com.protone.database.room

import android.content.Context
import com.protone.database.room.dao.*
import com.wajahatkarim3.roomexplorer.RoomExplorer

fun getGalleyDAO(): SignedGalleyDAO {
    return SeennDataBase.database.getGalleyDAO()
}

fun getNoteDAO(): NoteDAO {
    return SeennDataBase.database.getNoteDAO()
}

fun getNoteTypeDAO(): NoteTypeDAO {
    return SeennDataBase.database.getNoteTypeDAO()
}

fun getMusicBucketDAO(): MusicBucketDAO {
    return SeennDataBase.database.getMusicBucketDAO()
}

fun getMusicDAO(): MusicDAO {
    return SeennDataBase.database.getMusicDAO()
}

fun getGalleyBucketDAO(): GalleyBucketDAO {
    return SeennDataBase.database.getGalleyBucketDAO()
}

fun getGalleriesWithNotesDAO(): GalleriesWithNotesDAO {
    return SeennDataBase.database.getGalleriesWithNotesDAO()
}

fun getNoteDirWithNoteDAO(): NoteDirWithNoteDAO {
    return SeennDataBase.database.getNoteDirWithNoteDAO()
}

fun getMusicWithMusicBucketDAO(): MusicWithMusicBucketDAO {
    return SeennDataBase.database.getMusicWithMusicBucketDAO()
}

fun showRoomDB(context: Context){
    RoomExplorer.show(context,SeennDataBase::class.java,"SeennDB")
}
