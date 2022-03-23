package com.protone.database.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Note {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    public int id;

    @ColumnInfo(name = "Title")
    public String title;

    @ColumnInfo(name = "Text")
    public String text;

    @ColumnInfo(name = "TitleImage")
    public String iMGUri;

    @ColumnInfo(name = "Time")
    public String time;

    public String type;

    public Note(String title, String text, String iMGUri, String time, String type) {
        this.title = title;
        this.text = text;
        this.iMGUri = iMGUri;
        this.time = time;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getiMGUri() {
        return iMGUri;
    }

    public void setiMGUri(String iMGUri) {
        this.iMGUri = iMGUri;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
