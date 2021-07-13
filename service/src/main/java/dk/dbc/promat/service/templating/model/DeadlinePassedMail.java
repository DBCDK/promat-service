package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.persistence.PromatCase;
import java.util.List;

public class DeadlinePassedMail {
    PromatCase promatCase;
    List<BibliographicInformation> titleSections;

    public PromatCase getPromatCase() {
        return promatCase;
    }

    public void setPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
    }

    public DeadlinePassedMail withPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
        return this;
    }

    public List<BibliographicInformation> getTitleSections() {
        return titleSections;
    }

    public void setTitleSections(List<BibliographicInformation> titleSections) {
        this.titleSections = titleSections;
    }

    public DeadlinePassedMail withTitleSections(List<BibliographicInformation> titleSections) {
        this.titleSections = titleSections;
        return this;
    }
}