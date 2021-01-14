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
        return "Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder\n" +
                paymentList.getPayments()
                        .stream()
                        .map(payment -> String.format("%s;%s;%s;%d;%s;%s\n",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                                payment.getPayCode(),
                                payment.getPayCategory(),
                                payment.getCount(),
                                payment.getText(),
                                payment.getReviewer().getFirstName() + " " + payment.getReviewer().getLastName()))
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
                        .withPayCategory("5678")
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
