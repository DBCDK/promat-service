package dk.dbc.promat.service.taxonomy.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class Subject implements Serializable {
    String title;
    List<String> note;
    boolean oftenUsed;
    int id;


    public String getTitle() {
        return title;
    }

    public Subject withTitle(String title) {
        this.title = title;
        return this;
    }

    public List<String> getNote() {
        return note;
    }

    public Subject withNote(List<String> note) {
        this.note = note;
        return this;
    }

    public boolean isOftenUsed() {
        return oftenUsed;
    }

    public Subject withOftenUsed(boolean oftenUsed) {
        this.oftenUsed = oftenUsed;
        return this;
    }

    public int getId() {
        return id;
    }

    public Subject withId(int id) {
        this.id = id;
        return this;
    }

    @SuppressWarnings("unchecked")
    public static Subject of(HashMap<String, Object> map) {
        Subject subject = new Subject();
        subject.title = (String) map.get("title");
        subject.note = (List<String>) map.get("note");
        subject.oftenUsed = (boolean) map.get("oftenUsed");
        subject.id = (int) map.get("id");
        return subject;
    }

    public LinkedHashMap<String, Object> toHashMap() {
        LinkedHashMap<String, Object> hashMap = new LinkedHashMap<>();
        hashMap.put("title", title);
        hashMap.put("note", note);
        hashMap.put("oftenUsed", oftenUsed);
        hashMap.put("id", id);
        return hashMap;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return oftenUsed == subject.oftenUsed && id == subject.id && Objects.equals(title, subject.title) && Objects.equals(note, subject.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, note, oftenUsed, id);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "title='" + title + '\'' +
                ", note=" + note +
                ", oftenUsed=" + oftenUsed +
                ", id=" + id +
                '}';
    }
}
