/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.GroupedPaymentList;
import dk.dbc.promat.service.dto.Payment;
import dk.dbc.promat.service.dto.PaymentList;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
        PAYMENT_LIST_BY_USER, //Todo: May be superfluous, waiting on ux/frontend
        PAYMENT_LIST_BY_FAUST //Todo: ...^^
    }

    @GET
    @Path("payments/preview")
    public Response preview(@QueryParam("format") final PaymentsFormat selectedformat) {

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
                        .entity(mapper.writerWithView(CaseView.Summary.class)
                                .writeValueAsString(groupPaymentsByUser(paymentList)))
                        .build();
            } else if(format == PaymentsFormat.PAYMENT_LIST_BY_FAUST) {
                ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/json")
                        .entity(mapper.writerWithView(CaseView.Summary.class)
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
        return "Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder\n" +
                paymentList.getPayments()
                        .stream()
                        .map(payment -> String.format("%s;%s;%s;%d;%s;%s\n",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                                payment.getPayCode(),
                                payment.getPayCategory().value(),
                                payment.getCount(),
                                payment.getText(),
                                payment.getReviewer().getFirstName() + " " + payment.getReviewer().getLastName()))
                        .collect(Collectors.joining());
    }

    private PaymentList getPendingPayments(boolean execute) throws ServiceErrorException {

        // Lock all relevant tables
        if(execute) {
            LOGGER.info("Locking case and task tables for payment execution");
            repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
            repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);
        }

        final TypedQuery<PromatCase> query = entityManager.createNamedQuery(
                PromatCase.GET_CASES_FOR_PAYMENT_NAME, PromatCase.class);

        List<Payment> payments = new ArrayList<>();
        for (PromatCase promatCase : query.getResultList()) {

            // Check for serious inconsistencies
            if (promatCase.getReviewer() == null) {
                LOGGER.error(String.format("Case id %d has reviewer=null", promatCase.getId()));
                throw new ServiceErrorException("Case ready for payment has no reviewer assigned")
                        .withHttpStatus(500)
                        .withCode(ServiceErrorCode.FAILED)
                        .withDetails(String.format("Case id %d has reviewer=null", promatCase.getId()));
            }

            // Add case payments
            if (promatCase.getTasks() != null) {

                List<PayCategory> casePayCategories = new ArrayList<>();

                // Count payable fields
                for (PromatTask task : promatCase.getTasks()) {

                    // Skip tasks that has not been approved and tasks that has been payed
                    if (task.getApproved() == null) {
                        continue;
                    }
                    if (task.getPayed() != null) {
                        continue;
                    }

                    switch (task.getPayCategory()) {
                        case BRIEF:
                        case METAKOMPAS:
                        case BKM:
                            // These fields is payed by quantity
                            casePayCategories.add(task.getPayCategory());
                            break;
                        default:
                            // These fields is payed once, disregarding the number of fields
                            if (!casePayCategories.contains(task.getPayCategory())) {
                                casePayCategories.add(task.getPayCategory());
                            }
                            break;
                    }
                }

                // Group the categories since we need to add lines with a count of items.
                // Also sort the categories so that they are added in ascending numerical order
                // in the list - this is not an outside requirement, but just to ensure that
                //   a) the payments within a case is placed in the logical order
                //   b) we can construct a integration test to check payment output
                Map<PayCategory, List<PayCategory>> groupedPayCategories = casePayCategories.stream()
                        .collect(Collectors.groupingBy(category -> category));
                List<PayCategory> sortedPayCategories = groupedPayCategories
                        .keySet()
                        .stream()
                        .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.value())))
                        .collect(Collectors.toList());

                // Insert payment lines
                for (PayCategory payCategory : sortedPayCategories) {
                    payments.add(new Payment()
                            .withPayCode(promatCase.getReviewer().getPaycode().toString())
                            .withPayCategory(payCategory)
                            .withCount(groupedPayCategories.get(payCategory).size())
                            .withText(String.format("%s%s %s", getFaustList(promatCase), getTextCategory(payCategory), promatCase.getTitle()))
                            .withReviewer(promatCase.getReviewer())
                            .withPrimaryFaust(promatCase.getPrimaryFaust())
                            .withRelatedFausts(promatCase.getRelatedFausts().stream().collect(Collectors.joining(",")))
                            .withTitle(promatCase.getTitle())
                            .withWeekCode(promatCase.getWeekCode())
                            .withMaterialType(promatCase.getMaterialType())
                            .withDeadline(promatCase.getDeadline()));
                }
            }

            // A case with status PENDING_CLOSE should have a BKM task which has been approved
            // and thus added to the payments. After adding the approved BKM task, set the case
            // status to closed.
            if (execute) {
                if(promatCase.getStatus() == CaseStatus.PENDING_CLOSE) {
                    LOGGER.info("Changing status of case {} to CLOSED due to BKM assessment requesting case being closed");
                    promatCase.setStatus(CaseStatus.CLOSED);
                }
            }
        }

        return new PaymentList()
                .withStamp(LocalDate.now())
                .withNumFound(payments.size())
                .withPayments(payments);
    }

    private String getTextCategory(PayCategory category) {
        switch (category) {
            case BRIEF:
                return " Note";
            case METAKOMPAS:
                return " Metadata";
            case BKM:
                return " Bkm";
        }
        return "";
    }

    private String getFaustList(PromatCase promatCase) {
        List<String> fausts = new ArrayList<>();
        fausts.add(promatCase.getPrimaryFaust());
        if (promatCase.getRelatedFausts().size() > 0) {
            fausts.addAll(promatCase.getRelatedFausts());
        }

        return fausts.stream().collect(Collectors.joining(","));
    }
}
