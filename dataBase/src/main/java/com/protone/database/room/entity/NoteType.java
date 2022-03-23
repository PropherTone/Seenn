package com.protone.database.room.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class NoteType {

    @PrimaryKey(autoGenerate = true)
    Integer _ID;

    String type;

    String image;

    public NoteType(String type, String image) {
        this.type = type;
        this.image = image;
    }

    public Integer get_ID() {
        return _ID;
    }

    public void set_ID(Integer _ID) {
        this._ID = _ID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
