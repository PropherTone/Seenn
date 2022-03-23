package com.protone.database.room.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.protone.database.room.converters.MusicTypeConverter

@Entity
data class MusicBucket(var name: String, var Icon: String?){
    @PrimaryKey(autoGenerate = true)
    var id : Long = 0
}