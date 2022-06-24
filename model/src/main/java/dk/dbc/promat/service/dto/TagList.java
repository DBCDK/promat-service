package dk.dbc.promat.service.dto;

import java.util.Arrays;
import java.util.List;

public class TagList {
    private List<String> tags;

    public TagList() {
    }

    public TagList(String... tags) {
        this.tags = Arrays.asList(tags);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
