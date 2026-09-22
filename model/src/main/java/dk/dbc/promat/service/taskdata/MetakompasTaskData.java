package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

// The full shape of a METAKOMPAS task's selection - resolved entries plus free-text suggestions -
// used unchanged both for what's persisted in PromatTask.data and what PUT /tasks/{taskId}/metakompas
// returns. Entry/Suggestion are nested here (like java.util.Map.Entry) since neither exists outside
// a selection; reference them qualified (MetakompasTaskData.Entry), since "Entry" alone reads poorly.
public class MetakompasTaskData {
    private List<Entry> entries;
    private List<Suggestion> suggestions;

    public MetakompasTaskData() {
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public MetakompasTaskData withEntries(List<Entry> entries) {
        this.entries = entries;
        return this;
    }

    public MetakompasTaskData withSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasTaskData that = (MetakompasTaskData) o;
        return Objects.equals(entries, that.entries) && Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, suggestions);
    }

    @Override
    public String toString() {
        return "MetakompasTaskData{" +
                "entries=" + entries +
                ", suggestions=" + suggestions +
                '}';
    }

    // A subject picked by a reviewer, stored as-is rather than as a live tree reference, so
    // restructuring the taxonomy later never requires migrating old selections. oftenUsed/ref
    // are already public via GET /taxonomy/tree, so nothing here needs hiding from the client.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(List<String> path, Integer id, String title, List<String> note,
                        Boolean oftenUsed, String ref) {
    }

    // A reviewer-typed suggestion for a word not (yet) in the taxonomy - never resolved or
    // assigned an id.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(List<String> path, String text) {
    }
}
