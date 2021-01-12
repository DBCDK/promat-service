/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@Path("")
public class Payments {
    private static final Logger LOGGER = LoggerFactory.getLogger(Payments.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    public enum PaymentsFormat {
        CSV,
        PAYMENT_LIST,
        PAYMENT_LIST_BY_USER,
        PAYMENT_LIST_BY_FAUST
    }

    private class Payment {

        String payCode;

        String payType;

        int count;

        String text;

        @JsonView({CaseView.CaseSummary.class})
        Reviewer reviewer;

        String primaryFaust;

        String title;

        String weekCode;

        TaskFieldType taskFieldType;

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

        public Reviewer getReviewer() {
            return reviewer;
        }

        public void setReviewer(Reviewer reviewer) {
            this.reviewer = reviewer;
        }

        public String getPrimaryFaust() {
            return primaryFaust;
        }

        public void setPrimaryFaust(String primaryFaust) {
            this.primaryFaust = primaryFaust;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getWeekCode() {
            return weekCode;
        }

        public void setWeekCode(String weekCode) {
            this.weekCode = weekCode;
        }

        public TaskFieldType getTaskFieldType() {
            return taskFieldType;
        }

        public void setTaskFieldType(TaskFieldType taskFieldType) {
            this.taskFieldType = taskFieldType;
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

        public Payment withReviewer(Reviewer reviewer) {
            this.reviewer = reviewer;
            return this;
        }

        public Payment withPrimaryFaust(String primaryFaust) {
            this.primaryFaust = primaryFaust;
            return this;
        }

        public Payment withTitle(String title) {
            this.title = title;
            return this;
        }

        public Payment withWeekCode(String weekCode) {
            this.weekCode = weekCode;
            return this;
        }

        public Payment withTaskFieldType(TaskFieldType taskFieldType) {
            this.taskFieldType = taskFieldType;
            return this;
        }
    }

    private class PaymentList {

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

    private class GroupedPaymentList {

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

    @GET
    @Path("payments/preview")
    public Response preview(@QueryParam("format") final PaymentsFormat selectedformat) {
        LOGGER.info("ENTRY");
        PaymentsFormat format = selectedformat != null ? selectedformat : PaymentsFormat.PAYMENT_LIST;
        LOGGER.info("payments/preview/?format={} (GET)", selectedformat);

        PaymentList paymentList;
        try {
            paymentList = getPendingPayments(false);
            LOGGER.info("Has {} pending payments", paymentList.getNumFound());
        }
        catch(Exception e) {
            LOGGER.error("Unexpected exception while retrieving payments: {}", e.getMessage());
            return ServiceErrorDto.Failed("Unexpected exception while retrieving payments");
        }

        try {
            if(format == PaymentsFormat.CSV) {
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/csv")
                        .header("Content-Disposition", "attachment; filename=" + getPaymentsCsvFilename("promat_payments_PREVIEW"))
                        .entity(convertPaymentListToCsv(paymentList))
                        .build();
            } else if(format == PaymentsFormat.PAYMENT_LIST) {
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/json")
                        .entity(paymentList)
                        .build();
            } else if(format == PaymentsFormat.PAYMENT_LIST_BY_USER) {
                ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/json")
                        .entity(mapper.writerWithView(CaseView.CaseSummary.class)
                                .writeValueAsString(groupPaymentsByUser(paymentList)))
                        .build();
            } else if(format == PaymentsFormat.PAYMENT_LIST_BY_FAUST) {
                ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/json")
                        .entity(mapper.writerWithView(CaseView.CaseSummary.class)
                                .writeValueAsString(groupPaymentsByFaust(paymentList)))
                        .build();
            } else {
                // Aka. "Developer note": "You forgot something!"
                LOGGER.info("Payment format {} is not handled by preview()", format);
                return ServiceErrorDto.InvalidRequest("Unknown format", "The given format is not handled by this endpoint");
            }
        }
        catch(Exception e) {
            LOGGER.error("Unexpected exception while building payments list: {}", e.getMessage());
            return ServiceErrorDto.Failed("Unexpected exception while building payments list");
        }
    }

    private GroupedPaymentList groupPaymentsByUser(PaymentList paymentList) {
        Map<Integer, List<Payment>> grouped = paymentList
                .getPayments()
                .stream()
                .collect(Collectors.groupingBy(payment -> payment.getReviewer().getId()));

        List<PaymentList> paymentLists = grouped
                .values()
                .stream()
                .map(group -> new PaymentList()
                        .withNumFound(group.size())
                        .withPayments(group))
                .collect(Collectors.toList());

        return new GroupedPaymentList()
                .withNumFound(paymentLists.size())
                .withGroups(paymentLists);
    }

    private GroupedPaymentList groupPaymentsByFaust(PaymentList paymentList) {
        Map<String, List<Payment>> grouped = paymentList
                .getPayments()
                .stream()
                .collect(Collectors.groupingBy(Payment::getPrimaryFaust));

        List<PaymentList> paymentLists = grouped
                .values()
                .stream()
                .map(group -> new PaymentList()
                        .withNumFound(group.size())
                        .withPayments(group))
                .collect(Collectors.toList());

        return new GroupedPaymentList()
                .withNumFound(paymentLists.size())
                .withGroups(paymentLists);
    }

    private String getPaymentsCsvFilename(String prefix) {
        return prefix + String.format("_%s", LocalDateTime.now()
                .format(DateTimeFormatter
                        .ofPattern("yyyyMMdd_HHmmssSSS"))) + ".csv";
    }

    private String convertPaymentListToCsv(PaymentList paymentList) {
        return "Lønnr;Lønart;Antal;;;Tekst\n" +
                paymentList.getPayments()
                        .stream()
                        .map(payment -> String.format("%s;%s;%d;%s\n",
                                payment.getPayCode(),
                                payment.getPayType(),
                                payment.getCount(),
                                payment.getText()))
                        .collect(Collectors.joining());
    }

    private PaymentList getPendingPayments(boolean execute) {

        // Lock all relevant tables
        if(execute) {
            LOGGER.info("Locking case and task tables for payment execution");
            repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
            repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);
        }

        // Todo: Example data, will be dynamiccaly built
        final PaymentList paymentList = new PaymentList()
                .withNumFound(1)
                .withPayments(Arrays.asList(new Payment()
                        .withPayCode("1234")
                        .withPayType("5678")
                        .withCount(3)
                        .withText("example payment")
                        .withReviewer(entityManager.find(Reviewer.class, 1))
                        .withPrimaryFaust("123")
                        .withTitle("title")
                        .withWeekCode("BKM202101")
                        .withTaskFieldType(TaskFieldType.BRIEF)));

        return paymentList;
    }
}
