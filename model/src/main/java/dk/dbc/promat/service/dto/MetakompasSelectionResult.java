package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

// The client-facing counterpart of MetakompasSelectionData: the same shape, but with each
// entry trimmed to MetakompasSelectionView (no oftenUsed/ref) - what tasks/{taskId}/metakompas
// actually returns.
public class MetakompasSelectionResult {
    private List<MetakompasSelectionView> entries;
    private List<MetakompasSuggestion> suggestions;

    public MetakompasSelectionResult() {
    }

    public List<MetakompasSelectionView> getEntries() {
        return entries;
    }

    public void setEntries(List<MetakompasSelectionView> entries) {
        this.entries = entries;
    }

    public List<MetakompasSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<MetakompasSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public MetakompasSelectionResult withEntries(List<MetakompasSelectionView> entries) {
        this.entries = entries;
        return this;
    }

    public MetakompasSelectionResult withSuggestions(List<MetakompasSuggestion> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelectionResult that = (MetakompasSelectionResult) o;
        return Objects.equals(entries, that.entries) && Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, suggestions);
    }

    @Override
    public String toString() {
        return "MetakompasSelectionResult{" +
                "entries=" + entries +
                ", suggestions=" + suggestions +
                '}';
    }
}
