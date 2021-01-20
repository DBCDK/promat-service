/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentList {

    private int numFound;

    private List<Payment> payments = new ArrayList<>();

    private LocalDateTime stamp;

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

    public LocalDateTime getStamp() {
        return stamp;
    }

    public void setStamp(LocalDateTime stamp) {
        this.stamp = stamp;
    }

    public PaymentList withNumFound(int numFound) {
        this.numFound = numFound;
        return this;
    }

    public PaymentList withPayments(List<Payment> payments) {
        this.payments = payments;
        return this;
    }

    public PaymentList withStamp(LocalDateTime stamp) {
        this.stamp = stamp;
        return this;
    }
}
