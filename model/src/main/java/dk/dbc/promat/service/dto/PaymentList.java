/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import java.util.ArrayList;
import java.util.List;

public class PaymentList {

    private int numFound;

    private List<Payment> payments = new ArrayList<>();

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public PaymentList withNumFound(int numFound) {
        this.numFound = numFound;
        return this;
    }

    public PaymentList withPayments(List<Payment> payments) {
        this.payments = payments;
        return this;
    }
}
