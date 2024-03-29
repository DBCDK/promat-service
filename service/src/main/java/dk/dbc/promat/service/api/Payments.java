package dk.dbc.promat.service.api;

import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.Payment;
import dk.dbc.promat.service.dto.PaymentList;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        PAYMENT_LIST
    }

    public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmssSSS";

    @GET
    @Path("payments/preview")
    public Response preview(@QueryParam("format") final PaymentsFormat selectedformat,
                            @QueryParam("stamp") final String stamp) {

        PaymentsFormat format = selectedformat != null ? selectedformat : PaymentsFormat.PAYMENT_LIST;
        LOGGER.info("payments/preview/?format={}&stamp={} (GET)", selectedformat, stamp);

        PaymentList paymentList;
        try {
            paymentList = getPendingPayments(false, stamp);
            LOGGER.info("Has {} pending payments", paymentList.getNumFound());
        }
        catch(ServiceErrorException se) {
            LOGGER.error("ServiceErrorException thrown while fetching the payment list: {}", se.getServiceErrorDto().getDetails());
            return Response.status(se.getHttpStatus())
                    .header("Content-Type", "application/json")
                    .entity(se.getServiceErrorDto()).build();
        }
        catch(Exception e) {
            LOGGER.error("Unexpected exception while retrieving payments: {}", e.getMessage());
            return Response.status(500)
                    .header("Content-Type", "application/json")
                    .entity(e.getMessage()).build();
        }

        try {
            if(format == PaymentsFormat.CSV) {
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/csv; charset=ISO-8859-1")
                        .header("Content-Disposition", "attachment; filename=" + getPaymentsCsvFilename("promat_payments_PREVIEW.csv"))
                        .entity(convertPaymentListToCsv(paymentList).getBytes(StandardCharsets.ISO_8859_1))
                        .build();
            } else if(format == PaymentsFormat.PAYMENT_LIST) {
                return Response.status(200)
                        .header("Pragma", "no-cache")
                        .header("Content-Type", "application/json")
                        .entity(paymentList)
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

    @PUT
    @Path("payments/execute")
    public Response execute() {
        LOGGER.info("payments/execute (PUT)");

        PaymentList paymentList;
        try {
            paymentList = getPendingPayments(true, null);
            LOGGER.info("Has {} payments", paymentList.getNumFound());
        } catch(ServiceErrorException se) {
            LOGGER.error("ServiceErrorException thrown while executing payments: {}", se.getServiceErrorDto().getDetails());
            throw new WebApplicationException(se,
                    Response.status(se.getHttpStatus())
                            .header("Content-Type", "application/json")
                            .entity(se.getServiceErrorDto()).build());

        } catch(Exception e) {
            LOGGER.error("Unexpected exception thrown while executing payments: {}", e.getMessage());
            throw new WebApplicationException(e,
                    Response.status(500)
                            .header("Content-Type", "application/json")
                            .entity(e.getMessage()).build());
        }

        return Response.status(200)
                .header("Pragma", "no-cache")
                .header("Content-Type", "application/csv; charset=ISO-8859-1")
                .header("Content-Disposition", "attachment; filename=" +
                        getPaymentsCsvFilename(String.format("promat_payments_%s.csv", paymentList.getStamp())))
                .entity(convertPaymentListToCsv(paymentList).getBytes(StandardCharsets.ISO_8859_1))
                .build();
    }

    @GET
    @Path("payments/history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preview() {
        LOGGER.info("payments/history (GET)");

        final TypedQuery<LocalDateTime> query = entityManager.createNamedQuery(
                PromatTask.GET_PAYMENT_HISTORY_NAME, LocalDateTime.class);

        return Response.status(200)
                .entity(query.getResultList().stream()
                        .map(stamp -> stamp.format(DateTimeFormatter
                                .ofPattern(TIMESTAMP_FORMAT)))
                        .collect(Collectors.toList()))
                .build();
    }

    private String getPaymentsCsvFilename(String prefix) {
        return prefix + String.format("_%s", LocalDateTime.now()
                .format(DateTimeFormatter
                        .ofPattern(TIMESTAMP_FORMAT))) + ".csv";
    }

    private String convertPaymentListToCsv(PaymentList paymentList) {
        return "Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder\n" +
                paymentList.getPayments()
                        .stream()
                        .map(payment -> String.format("%s;%s;%s;%d;%s;%s\n",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                                payment.getPayCode(),
                                payment.getPayCategoryCode(),
                                payment.getCount(),
                                payment.getText(),
                                payment.getReviewer().getFirstName() + " " + payment.getReviewer().getLastName()))
                        .collect(Collectors.joining());
    }

    private PaymentList getPendingPayments(boolean execute, String stamp) throws ServiceErrorException {

        // Lock all relevant tables
        if(execute) {
            LOGGER.info("Locking case and task tables for payment execution");
            repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
            repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);
        }

        // Get the tasks that has been payed / can be payed
        PaymentList paymentList = new PaymentList();
        List<PromatCase> payable = getPayable(paymentList, stamp);

        List<Payment> payments = new ArrayList<>();
        for (PromatCase promatCase : payable) {

            // Check for serious inconsistencies
            if (promatCase.getReviewer() == null) {
                LOGGER.error(String.format("Case id %d has reviewer=null", promatCase.getId()));
                throw new ServiceErrorException("Case ready for payment has no reviewer assigned")
                        .withHttpStatus(500)
                        .withCode(ServiceErrorCode.FAILED)
                        .withDetails(String.format("Case id %d has reviewer=null", promatCase.getId()));
            }

            // Check that all tasks has been approved before starting payment, unless the case
            // has status PENDING_CLOSE where it is expected that only the BKM task has been approved
            if ( promatCase.getStatus() != CaseStatus.PENDING_CLOSE && promatCase.getStatus() != CaseStatus.CLOSED &&
                    promatCase.getTasks().stream().anyMatch(t -> t.getApproved() == null)) {
                LOGGER.error(String.format("Case id %d has task(s) that has not been approved allthough the case has status APPROVED or better", promatCase.getId()));
                throw new ServiceErrorException("Case ready for payment has not-approved tasks")
                        .withHttpStatus(500)
                        .withCode(ServiceErrorCode.FAILED)
                        .withDetails(String.format("Case id %d task(s) that has not been approved", promatCase.getId()));
            }

            // Add case payments
            if (promatCase.getTasks() != null) {

                List<PayCategory> casePayCategories = new ArrayList<>();

                // Count payable fields
                for (PromatTask task : promatCase.getTasks()) {

                    // Due to the query that returns entire cases, we get tasks that either has not been approved
                    // or payed, or has been payed already. These must be filtered out!
                    if (!isPayableTask(stamp != null, paymentList, task)) {
                        continue;
                    }

                    switch (task.getPayCategory()) {
                        case BRIEF:
                            // This field is payed by quantity unless it targets the primary faust, then it
                            // must count as part of the base review
                            if (task.getTargetFausts().contains(promatCase.getPrimaryFaust())) {
                                PayCategory payCategory = Repository.getPayCategoryForTaskType(task.getTaskType());
                                if( payCategory == PayCategory.NONE ) {
                                    LOGGER.error("Found approved and nonpayed task {} with paycategory 'NONE' (due to tasktype or taskfieldtype being 'NONE'). Task will not be payed", task.getId());
                                    continue;
                                }
                                if (!casePayCategories.contains(payCategory)) {
                                    casePayCategories.add(payCategory);
                                }
                            } else {
                                casePayCategories.add(task.getPayCategory());
                            }
                            break;
                        case METAKOMPAS:
                        case BUGGI:
                        case BKM:
                            // These fields is payed by quantity
                            casePayCategories.add(task.getPayCategory());
                            break;
                        case EXPRESS:
                            // This field should be ignored
                            break;
                        default:
                            // These fields is payed once, disregarding the number of fields
                            if (!casePayCategories.contains(task.getPayCategory())) {
                                casePayCategories.add(task.getPayCategory());
                            }
                            break;
                    }

                    if (execute) {
                        if (task.getPayed() != null) {
                            throw new ServiceErrorException("Attempt to change payed for task which has already been paid")
                                    .withHttpStatus(500)
                                    .withCode(ServiceErrorCode.FAILED)
                                    .withDetails(String.format("Task id %d has been paid previously", task.getId()));
                        }
                        LOGGER.info("Task {} marked as paid with stamp {}", task.getId(),
                                paymentList.getStamp().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)));
                        task.setPayed(paymentList.getStamp());
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
                            .withText(String.format("%s%s %s", getFaustList(promatCase),
                                    getTextCategory(payCategory,groupedPayCategories.get(payCategory).size()),
                                    promatCase.getTitle()))
                            .withReviewer(promatCase.getReviewer())
                            .withPrimaryFaust(promatCase.getPrimaryFaust())
                            .withRelatedFausts(promatCase.getTasks().stream().flatMap(t -> t.getTargetFausts().stream()).collect(Collectors.joining(",")))
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
                    LOGGER.info("Changing status of case {} to CLOSED due to BKM assessment requesting case being closed", promatCase.getId());
                    promatCase.setStatus(CaseStatus.CLOSED);
                }
            }
        }

        // Add the payments and return the completed list
        paymentList.setNumFound(payments.size());
        paymentList.setPayments(payments);
        return paymentList;
    }

    private List<PromatCase> getPayable(PaymentList paymentList, String stamp) {
        final TypedQuery<PromatCase> query;
        if (stamp == null) {
            query = entityManager.createNamedQuery(PromatCase.GET_CASES_FOR_PAYMENT_NAME, PromatCase.class);
            paymentList.setStamp(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        } else {
            query = entityManager.createNamedQuery(PromatCase.GET_PAYED_CASES_NAME, PromatCase.class);
            LocalDateTime time = LocalDateTime.parse(stamp, DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
            query.setParameter("stamp", time);
            paymentList.setStamp(time);
        }
        return query.getResultList();
    }

    private boolean isPayableTask(boolean history, PaymentList paymentList, PromatTask task) {

        // Check approved and/or payed - this differs for new and old payments
        if (!history) {
            if(task.getApproved() == null) { // Skip tasks that has not been approved
                return false;
            }
            if(task.getPayed() != null) {    // Skip tasks that has been payed already.
                return false;
            }
        } else {
            if (task.getPayed() == null ||
                    !task.getPayed().equals(paymentList.getStamp())) { // Skip tasks that does not match the stamp
                return false;
            }
        }
        return true;
    }

    private String getTextCategory(PayCategory category, int count) {
        switch (category) {
            case BRIEF:
                return " Kort om, +" + count;
            case METAKOMPAS:
                return " Metadata";
            case BKM:
                return " Bkm";
        }
        return "";
    }

    private String getFaustList(PromatCase promatCase) {
        Stream<String> caseFausts = promatCase.getTasks().stream().map(PromatTask::getTargetFausts).flatMap(Collection::stream);
        return Stream.concat(Stream.of(promatCase.getPrimaryFaust()), caseFausts)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }
}
