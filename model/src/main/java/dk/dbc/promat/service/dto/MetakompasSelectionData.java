package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

// The full shape persisted into PromatTask.data for a METAKOMPAS task: resolved entries (with
// oftenUsed/ref, needed internally) plus any free-text suggestions for words not in the
// taxonomy. See MetakompasSelectionResult for the client-facing counterpart.
public class MetakompasSelectionData {
    private List<MetakompasSelectionEntry> entries;
    private List<MetakompasSuggestion> suggestions;

    public MetakompasSelectionData() {
    }

    public List<MetakompasSelectionEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<MetakompasSelectionEntry> entries) {
        this.entries = entries;
    }

    public List<MetakompasSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<MetakompasSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public MetakompasSelectionData withEntries(List<MetakompasSelectionEntry> entries) {
        this.entries = entries;
        return this;
    }

    public MetakompasSelectionData withSuggestions(List<MetakompasSuggestion> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelectionData that = (MetakompasSelectionData) o;
        return Objects.equals(entries, that.entries) && Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, suggestions);
    }

    @Override
    public String toString() {
        return "MetakompasSelectionData{" +
                "entries=" + entries +
                ", suggestions=" + suggestions +
                '}';
    }
}
