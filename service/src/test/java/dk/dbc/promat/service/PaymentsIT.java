/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.GroupedPaymentList;
import dk.dbc.promat.service.dto.PaymentList;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PaymentsIT  extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentsIT.class);

    @Test
    public void TestGetPaymentsAsCvs() {
        assertThat("status code", getResponse("v1/api/payments/preview", Map.of("format","CSV")).getStatus(), is(200));
        //Todo: Check that the list has payments
    }

    @Test
    public void TestGetPaymentsAsPaymentList() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST"));
        assertThat("status code", response.getStatus(), is(200));

        PaymentList created = mapper.readValue(response.readEntity(String.class), PaymentList.class);
        //Todo: Check that the list has payments
    }

    @Test
    public void TestGetPaymentsAsPaymentListByUser() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST_BY_USER"));
        assertThat("status code", response.getStatus(), is(200));

        GroupedPaymentList created = mapper.readValue(response.readEntity(String.class), GroupedPaymentList.class);
        //Todo: Check that the list has groups
        //Todo: Check that the groups has payments
    }

    @Test
    public void TestGetPaymentsAsPaymentListByFaust() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST_BY_FAUST"));
        assertThat("status code", response.getStatus(), is(200));

        GroupedPaymentList created = mapper.readValue(response.readEntity(String.class), GroupedPaymentList.class);
        //Todo: Check that the list has groups
        //Todo: Check that the groups has payments
    }
}
