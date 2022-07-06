package dk.dbc.promat.service.dto;

import java.util.Arrays;
import java.util.List;

public class TagList {
    private List<Tag> tags;

    public TagList() {
    }

    public TagList(Tag... tags) {
        this.tags = Arrays.asList(tags);
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "TagList{" +
                "tags=" + tags +
                '}';
    }

}
