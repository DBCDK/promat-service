package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.taskdata.MetakompasSuggestion;

import java.util.List;
import java.util.Objects;

// A reviewer's full Metakompas selection for a task: the ids of existing taxonomy words picked
// (globally unique, so no path is needed to resolve them) and any free-text suggestions for
// words not (yet) in the taxonomy - each suggestion carries its own path, since that's the only
// place a path is actually needed.
public class MetakompasSelectionRequest {
    private List<Integer> ids;
    private List<MetakompasSuggestion> suggestions;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public List<MetakompasSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<MetakompasSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public MetakompasSelectionRequest withIds(List<Integer> ids) {
        this.ids = ids;
        return this;
    }

    public MetakompasSelectionRequest withSuggestions(List<MetakompasSuggestion> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelectionRequest that = (MetakompasSelectionRequest) o;
        return Objects.equals(ids, that.ids) && Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, suggestions);
    }

    @Override
    public String toString() {
        return "MetakompasSelectionRequest{" +
                "ids=" + ids +
                ", suggestions=" + suggestions +
                '}';
    }
}
