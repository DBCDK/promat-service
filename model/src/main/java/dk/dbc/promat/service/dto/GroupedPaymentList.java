/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import java.util.ArrayList;
import java.util.List;

public class GroupedPaymentList {

    private int numFound;

    private List<PaymentList> groups = new ArrayList<>();

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    public List<PaymentList> getGroups() {
        return groups;
    }

    public void setGroups(List<PaymentList> groups) {
        this.groups = groups;
    }

    public GroupedPaymentList withNumFound(int numFound) {
        this.numFound = numFound;
        return this;
    }

    public GroupedPaymentList withGroups(List<PaymentList> groups) {
        this.groups = groups;
        return this;
    }
}
