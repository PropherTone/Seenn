package com.protone.api.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class NoteDir {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "noteDirId")
    Long noteDirId;

    String name;

    String image;

    public NoteDir(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public Long getNoteDirId() {
        return noteDirId;
    }

    public void setNoteDirId(Long noteDirId) {
        this.noteDirId = noteDirId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
