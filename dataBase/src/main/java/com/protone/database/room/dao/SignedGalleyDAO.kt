package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.protone.database.room.entity.GalleyMedia

@Dao
interface SignedGalleyDAO {

    @Query("SELECT * FROM GalleyMedia")
    fun getAllSignedMedia() : List<GalleyMedia>?

    @Delete
    fun deleteSignedMedia(media: GalleyMedia)

    @Insert
    fun insertSignedMedia(media: GalleyMedia)

}