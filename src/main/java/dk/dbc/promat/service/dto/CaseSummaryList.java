package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatCase;

import java.util.ArrayList;
import java.util.List;

public class CaseSummaryList {

    private List<PromatCase> cases = new ArrayList<>();

    public int getNumFound() {
        return cases.size();
    }

    public List<PromatCase> getCases() {
        return cases;
    }

    public void setCases(List<PromatCase> cases) {
        this.cases = cases;
    }

    public CaseSummaryList withCases(List<PromatCase> cases) {
        this.cases = cases;
        return this;
    }
}
