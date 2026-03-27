package dk.dbc.promat.service.taxonomy.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Subject implements Serializable {
    String title;
    List<String> note = new ArrayList<>();
    boolean oftenUsed;
    int id = -1;

    String ref;


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

    public Subject withNote(Set<String> note) {
        this.note = note.stream().toList();
        return this;
    }

    public Subject withNote(String... note) {
        this.note = new ArrayList<>(Arrays.asList(note));
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
    public static Subject of(Map<String, Object> map) {
        Subject subject = new Subject();
        subject.title = (String) map.get("title");
        subject.note = (List<String>) map.get("note");
        subject.oftenUsed = (boolean) map.get("oftenUsed");
        subject.id = (int) map.get("id");
        subject.ref = (String) map.get("ref");
        return subject;
    }

    public LinkedHashMap<String, Object> toHashMap() {
        LinkedHashMap<String, Object> hashMap = new LinkedHashMap<>();
        hashMap.put("title", title);
        hashMap.put("note", note);
        hashMap.put("oftenUsed", oftenUsed);
        hashMap.put("id", id);
        Optional.ofNullable(ref).ifPresent(value -> hashMap.put("ref", value));
        return hashMap;
    }

    public String getRef() {
        return ref;
    }

    public Subject withRef(String ref) {
        this.ref = ref;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return oftenUsed == subject.oftenUsed && id == subject.id && Objects.equals(title, subject.title) && Objects.equals(note, subject.note) && Objects.equals(ref, subject.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, note, oftenUsed, id, ref);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "title='" + title + '\'' +
                ", note=" + note +
                ", oftenUsed=" + oftenUsed +
                ", id=" + id +
                ", ref='" + ref + '\'' +
                '}';
    }
}
