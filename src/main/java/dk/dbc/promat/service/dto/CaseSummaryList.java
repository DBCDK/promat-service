package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.Case;

import java.util.ArrayList;
import java.util.List;

public class CaseSummaryList {

    private int numFound = 0;

    private List<Case> cases = new ArrayList<>();

    public int getNumFound() {
        return cases.size();
    }

    public List<Case> getCases() {
        return cases;
    }

    public void setCases(List<Case> cases) {
        this.cases = cases;
    }

    public CaseSummaryList withCases(List<Case> cases) {
        this.cases = cases;
        return this;
    }
}
