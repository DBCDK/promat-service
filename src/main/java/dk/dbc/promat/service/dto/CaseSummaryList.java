/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatCase;

import java.util.ArrayList;
import java.util.List;

public class CaseSummaryList {

    private int numFound = 0;

    private List<PromatCase> cases = new ArrayList<>();

    public int getNumFound() {
        return cases.size();
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
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

    public CaseSummaryList withNumFound(int numFound) {
        this.numFound = numFound;
        return this;
    }
}
