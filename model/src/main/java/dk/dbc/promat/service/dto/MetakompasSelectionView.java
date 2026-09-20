package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

public class MetakompasSelectionView {
    private List<String> path;
    private Integer id;
    private String title;
    private List<String> note;

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public List<String> getNote() {
        return note;
    }

    public void setNote(List<String> note) {
        this.note = note;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MetakompasSelectionView withPath(List<String> path) {
        this.path = path;
        return this;
    }

    public MetakompasSelectionView withNote(List<String> note) {
        this.note = note;
        return this;
    }

    public MetakompasSelectionView withTitle(String title) {
        this.title = title;
        return this;
    }

    public MetakompasSelectionView withId(Integer id) {
        this.id = id;
        return this;
    }

    // The client-facing subset of a persisted MetakompasSelectionEntry - drops oftenUsed/ref,
    // which are only needed internally.
    public static MetakompasSelectionView from(MetakompasSelectionEntry entry) {
        return new MetakompasSelectionView()
                .withPath(entry.getPath())
                .withId(entry.getId())
                .withTitle(entry.getTitle())
                .withNote(entry.getNote());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelectionView that = (MetakompasSelectionView) o;
        return Objects.equals(path, that.path) && Objects.equals(id, that.id) && Objects.equals(title, that.title) && Objects.equals(note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, id, title, note);
    }

    @Override
    public String toString() {
        return "MetakompasSelectionView{" +
                "path=" + path +
                ", id=" + id +
                ", title='" + title + '\'' +
                ", note=" + note +
                '}';
    }
}
