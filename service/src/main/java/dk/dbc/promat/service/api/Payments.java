/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Stateless
@Path("payments")
public class Payments {
    private static final Logger LOGGER = LoggerFactory.getLogger(Payments.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    enum PaymentsFormat {
        CSV,
        JSON  // Todo: need a better name methinks.. but awaiting ux input before deciding
    }

    private class Payment {
        // Todo: This class will be refined as usage is clarified

        String payCode;
        String payType;
        int count;
        String text;

        public String getPayCode() {
            return payCode;
        }

        public void setPayCode(String payCode) {
            this.payCode = payCode;
        }

        public String getPayType() {
            return payType;
        }

        public void setPayType(String payType) {
            this.payType = payType;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Payment withPayCode(String payCode) {
            this.payCode = payCode;
            return this;
        }

        public Payment withPayType(String payType) {
            this.payType = payType;
            return this;
        }

        public Payment withCount(int count) {
            this.count = count;
            return this;
        }

        public Payment withText(String text) {
            this.text = text;
            return this;
        }
    }

    private class PaymentList {

        private int numFound;
        private List<Payment> payments = new ArrayList<Payment>();

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

    @GET
    @Path("preview")
    public Response preview(@QueryParam("format") PaymentsFormat format) {
        format = format != null ? format : PaymentsFormat.JSON;
        LOGGER.info("payments/preview/?format={} (GET)", format);

        // Lock all relevant tables
        // Todo: will move to central function, and applied only if needed
        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
        repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);

        if(format == PaymentsFormat.CSV) {
            // Todo: Example response, will be dynamiccaly built and formatted before this point
            String csv = "Lønnr;Lønart;Antal;;;Tekst\n";

            return Response.status(200)
                    .header("Content-Type", "application/csv")
                    .header("Content-Disposition", "attachment; filename=payments_preview.csv")  // Todo: Append calendar period to filename
                    .header("Pragma", "no-cache")
                    .entity(csv)
                    .build();
        } else {
            // Todo: Example response, will be dynamically built and formatted before this point
            final PaymentList paymentList = new PaymentList()
                    .withNumFound(1)
                    .withPayments(Arrays.asList(new Payment()
                            .withPayCode("1234")
                            .withPayType("5678")
                            .withCount(3)
                            .withText("example payment")));

            return Response.status(200)
                    .header("Content-Type", "application/json")
                    .header("Pragma", "no-cache")
                    .entity(paymentList)
                    .build();
        }
    }
}
