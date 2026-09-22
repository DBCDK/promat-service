package dk.dbc.promat.service.taskdata;

import java.util.List;
import java.util.Objects;

// The full shape of a METAKOMPAS task's selection: resolved entries plus any free-text
// suggestions for words not (yet) in the taxonomy. Used unchanged both as what's persisted in
// PromatTask.data and as what PUT /tasks/{taskId}/metakompas returns - see
// MetakompasSelectionEntry/MetakompasSuggestion for why the two roles don't need separate shapes.
public class MetakompasTaskData {
    private List<MetakompasSelectionEntry> entries;
    private List<MetakompasSuggestion> suggestions;

    public MetakompasTaskData() {
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

    public MetakompasTaskData withEntries(List<MetakompasSelectionEntry> entries) {
        this.entries = entries;
        return this;
    }

    public MetakompasTaskData withSuggestions(List<MetakompasSuggestion> suggestions) {
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
}
