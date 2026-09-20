package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

// One taxonomy path visited while tagging: the ids selected there (ids are globally unique, so
// path is not needed to resolve them - it travels along only because it's needed for the
// optional free-text suggestions, which have no id at all) and optional suggestions for words
// not (yet) in the taxonomy under that same path.
public class MetakompasSelectionRequest {
    private List<String> path;
    private List<Integer> ids;
    private List<String> suggestions;

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public MetakompasSelectionRequest withPath(List<String> path) {
        this.path = path;
        return this;
    }

    public MetakompasSelectionRequest withIds(List<Integer> ids) {
        this.ids = ids;
        return this;
    }

    public MetakompasSelectionRequest withSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelectionRequest that = (MetakompasSelectionRequest) o;
        return Objects.equals(path, that.path) && Objects.equals(ids, that.ids) && Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, ids, suggestions);
    }

    @Override
    public String toString() {
        return "MetakompasSelectionRequest{" +
                "path=" + path +
                ", ids=" + ids +
                ", suggestions=" + suggestions +
                '}';
    }
}
