package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

// A reviewer-typed suggestion for a word not (yet) in the Metakompas taxonomy, tied to the
// category path it was suggested under - never resolved against the taxonomy tree and never
// assigned an id.
public class MetakompasSuggestion {

    private List<String> path;
    private String text;

    public MetakompasSuggestion() {
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MetakompasSuggestion withPath(List<String> path) {
        this.path = path;
        return this;
    }

    public MetakompasSuggestion withText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSuggestion that = (MetakompasSuggestion) o;
        return Objects.equals(path, that.path) && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, text);
    }

    @Override
    public String toString() {
        return "MetakompasSuggestion{" +
                "path=" + path +
                ", text='" + text + '\'' +
                '}';
    }
}
