package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.persistence.PromatCase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignReviewer {
    private PromatCase promatCase;
    private String note;
    List<BibliographicInformation> titleSections;

    public AssignReviewer withPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
        return this;
    }

    public AssignReviewer withNote(String note) {
        this.note = note;
        return this;
    }

    public AssignReviewer withTitleSections(List<BibliographicInformation> titleSections) {
        this.titleSections = titleSections;
        return this;
    }

    public PromatCase getPromatCase() {
        return promatCase;
    }

    public String getNote() {
        return note;
    }

    public List<BibliographicInformation> getTitleSections() {
        return titleSections;
    }

    public void setPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTitleSections(List<BibliographicInformation> titleSections) {
        this.titleSections = titleSections;
    }
}