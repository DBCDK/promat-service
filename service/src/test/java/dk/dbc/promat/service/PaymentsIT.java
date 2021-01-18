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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PaymentsIT  extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentsIT.class);

    @Test
    public void TestGetPaymentsAsCvs() {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(200));

        String csv = response.readEntity(String.class);
        assertThat("number of line", csv.lines().count(), is(20L));  // 1 header + 19 paymentlines
    }

    @Test
    public void TestGetPaymentsAsPaymentList() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST"));
        assertThat("status code", response.getStatus(), is(200));

        PaymentList payments = mapper.readValue(response.readEntity(String.class), PaymentList.class);
        assertThat("number of paymentlines", payments.getPayments().size(), is(19));
    }

    @Test
    public void TestPaymentFile() {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(200));
        String csv = response.readEntity(String.class);
        assertThat("number of line", csv.lines().count(), is(20L));  // 1 header + 19 paymentlines

        verifyPaymentCsv(csv);
    }

    private void verifyPaymentCsv(String csv) {
        LOGGER.info("Received CSV output is:\n{}", csv);

        String expected = ("Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder\n" +
                "mm-dd-åååå;123;1960;1;1001000 Note Case 1;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001010 Note Case 2;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001020 Note Case 3;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001030 Note Case 4;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001040 Note Case 5;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001050 Note Case 6;Hans Hansen\n" +
                "mm-dd-åååå;123;1962;1;1001060 Bkm Case 7;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001070 Note Case 8;Hans Hansen\n" +
                "mm-dd-åååå;123;1962;1;1001070 Bkm Case 8;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;3;1001080,1001081,1001082,1001083 Note Case 9;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;3;1001090,1001091,1001092,1001093,1001094 Note Case 10;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;4;1001100,1001101,1001102,1001103,1001104 Note Case 11;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001110,1001111 Case 12;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;2;1001110,1001111 Note Case 12;Hans Hansen\n" +
                "mm-dd-åååå;123;1987;1;1001110,1001111 Metadata Case 12;Hans Hansen\n" +
                "mm-dd-åååå;456;1981;1;1001130,1001131 Case 14;Ole Olsen\n" +
                "mm-dd-åååå;456;1960;1;1001130,1001131 Note Case 14;Ole Olsen\n" +
                "mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Note Case 15;Ole Olsen\n" +
                "mm-dd-åååå;456;1960;1;1001150,1001151,1001152 Note Case 16;Ole Olsen")
                        .replace("mm-dd-åååå", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        LOGGER.info("Expected CSV output is:\n{}", expected);

        List<String> expectedLines = expected
                .lines()
                .collect(Collectors.toList());

        // Verify each line - one at the time, since it may become very confusing if we just
        // reported an error in the file without indicating which line was faulty
        int lineCnt = 1;
        for (String line : csv.lines().collect(Collectors.toList())) {
            LOGGER.info("Line {} actual   = {}", lineCnt, line);
            LOGGER.info("Line {} expected = {}", lineCnt, expectedLines.get(lineCnt - 1));
            assertThat("line is correct", line.equals(expectedLines.get(lineCnt - 1)));
            lineCnt++;
        }
    }

    // Todo: grouping is, most likely, not needed. If so, the test should be removed
    //@Test
    public void TestGetPaymentsAsPaymentListByUser() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST_BY_USER"));
        assertThat("status code", response.getStatus(), is(200));

        GroupedPaymentList created = mapper.readValue(response.readEntity(String.class), GroupedPaymentList.class);
        //Todo: Check that the list has groups
        //Todo: Check that the groups has payments
    }

    // Todo: grouping is, most likely, not needed. If so, the test should be removed
    //@Test
    public void TestGetPaymentsAsPaymentListByFaust() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST_BY_FAUST"));
        assertThat("status code", response.getStatus(), is(200));

        GroupedPaymentList created = mapper.readValue(response.readEntity(String.class), GroupedPaymentList.class);
        //Todo: Check that the list has groups
        //Todo: Check that the groups has payments
    }
}
