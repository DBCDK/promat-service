package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.persistence.PromatCase;
import java.util.HashMap;
import java.util.Map;

public class AssignReviewerNotification {
    private PromatCase promatCase;
    private String note;
    private Map<String, String> relatedFaustsTitles = new HashMap<>();

    public AssignReviewerNotification withPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
        return this;
    }

    public AssignReviewerNotification withRelatedFaustsTitles(Map<String, String> relatedFaustsTitles) {
        this.relatedFaustsTitles = relatedFaustsTitles;
        return this;
    }

    public AssignReviewerNotification withNote(String note) {
        this.note = note;
        return this;
    }

    public PromatCase getPromatCase() {
        return promatCase;
    }

    public String getNote() {
        return note;
    }

    public Map<String, String> getRelatedFaustsTitles() {
        return relatedFaustsTitles;
    }

    public void setPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setRelatedFaustsTitles(Map<String, String> relatedFaustsTitles) {
        this.relatedFaustsTitles = relatedFaustsTitles;
    }
}