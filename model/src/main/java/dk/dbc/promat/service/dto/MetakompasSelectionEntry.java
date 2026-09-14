package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

// One Metakompas subject picked by a reviewer for a task/faust, with its tree path attached.
// Deliberately decoupled from the taxonomy's hardcoded category names on the server side
// (dk.dbc.promat.service.taxonomy.dto.Taxonomy) - if those are ever renamed/restructured, old
// saved selections don't need migrating, they just carry a path that may no longer resolve.
public class MetakompasSelectionEntry {

    private List<String> path;
    private Integer id;
    private String title;
    private List<String> note;
    private Boolean oftenUsed;
    private String ref;

    public MetakompasSelectionEntry() {
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getNote() {
        return note;
    }

    public void setNote(List<String> note) {
        this.note = note;
    }

    public Boolean getOftenUsed() {
        return oftenUsed;
    }

    public void setOftenUsed(Boolean oftenUsed) {
        this.oftenUsed = oftenUsed;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public MetakompasSelectionEntry withPath(List<String> path) {
        this.path = path;
        return this;
    }

    public MetakompasSelectionEntry withId(Integer id) {
        this.id = id;
        return this;
    }

    public MetakompasSelectionEntry withTitle(String title) {
        this.title = title;
        return this;
    }

    public MetakompasSelectionEntry withNote(List<String> note) {
        this.note = note;
        return this;
    }

    public MetakompasSelectionEntry withOftenUsed(Boolean oftenUsed) {
        this.oftenUsed = oftenUsed;
        return this;
    }

    public MetakompasSelectionEntry withRef(String ref) {
        this.ref = ref;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelectionEntry that = (MetakompasSelectionEntry) o;
        return Objects.equals(path, that.path)
                && Objects.equals(id, that.id)
                && Objects.equals(title, that.title)
                && Objects.equals(note, that.note)
                && Objects.equals(oftenUsed, that.oftenUsed)
                && Objects.equals(ref, that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, id, title, note, oftenUsed, ref);
    }

    @Override
    public String toString() {
        return "MetakompasSelectionEntry{" +
                "path=" + path +
                ", id=" + id +
                ", title='" + title + '\'' +
                ", note=" + note +
                ", oftenUsed=" + oftenUsed +
                ", ref='" + ref + '\'' +
                '}';
    }
}
