package com.protone.database.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.protone.database.room.converters.ListTypeConverter;

import java.util.List;

@Entity
@TypeConverters(ListTypeConverter.class)
public class Note {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    public int id;

    @ColumnInfo(name = "Title")
    public String title;

    @ColumnInfo(name = "Text")
    public String text;

    @ColumnInfo(name = "TitleImage")
    public String imagePath;

    @ColumnInfo(name = "Time")
    public String time;

    public List<String> type;

    public int richCode;

    @Ignore
    public Note() {
    }

    public Note(String title, String text, String imagePath, String time, List<String> type, int richCode) {
        this.title = title;
        this.text = text;
        this.imagePath = imagePath;
        this.time = time;
        this.type = type;
        this.richCode = richCode;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getRichCode() {
        return richCode;
    }

    public void setRichCode(int richCode) {
        this.richCode = richCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Note note = (Note) o;

        if (id != note.id) return false;
        if (richCode != note.richCode) return false;
        if (!title.equals(note.title)) return false;
        if (!text.equals(note.text)) return false;
        if (!imagePath.equals(note.imagePath)) return false;
        if (!time.equals(note.time)) return false;
        return type.equals(note.type);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + title.hashCode();
        result = 31 * result + text.hashCode();
        result = 31 * result + imagePath.hashCode();
        result = 31 * result + time.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + richCode;
        return result;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", time='" + time + '\'' +
                ", type='" + type + '\'' +
                ", richCode=" + richCode +
                '}';
    }
}
