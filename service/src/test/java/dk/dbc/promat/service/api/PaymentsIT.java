package dk.dbc.promat.service.api;

 import com.fasterxml.jackson.core.JsonProcessingException;
 import dk.dbc.promat.service.ContainerTest;
 import dk.dbc.promat.service.connector.PromatServiceConnectorException;
 import dk.dbc.promat.service.dto.CaseRequest;
 import dk.dbc.promat.service.dto.PaymentList;
 import dk.dbc.promat.service.dto.ServiceErrorCode;
 import dk.dbc.promat.service.dto.ServiceErrorDto;
 import dk.dbc.promat.service.persistence.CaseStatus;
 import dk.dbc.promat.service.persistence.PromatCase;
 import org.junit.jupiter.api.MethodOrderer;
 import org.junit.jupiter.api.Order;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.TestMethodOrder;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import jakarta.ws.rs.core.Response;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.util.List;
 import java.util.Map;
 import java.util.stream.Collectors;

 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.Matchers.greaterThanOrEqualTo;
 import static org.hamcrest.core.Is.is;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentsIT  extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentsIT.class);

    // This test needs to run as the very first test to ensure that attempt at
    // executing payment fails (later tests will check that changes made before
    // payment was completed, has rolled back)
    @Test
    @Order(1)
    public void TestExecutePaymentShouldThrow() {
        Response response = putResponse("v1/api/payments/execute", "");
        assertThat("status code", response.getStatus(), is(500));
    }

    // Check how many payed tasks exists before modifying tasks
    @Order(2)
    @Test
    public void TestGetPreviousPaymentBeforeExecute() {

        Response response = getResponse("v1/api/payments/history");
        assertThat("status code", response.getStatus(), is(200));

        // Note: Case #20 is defined in promatcases.sql to test html caseview
        List<String> history = response.readEntity(List.class);
        assertThat("number of history lines is", history.size(), is(3));
    }

    // This test needs to run as the third test since it modifies the preloaded
    // cases to remove the invalid case from pending payments
    @Test
    @Order(3)
    public void TestGetPaymentsShouldThrowDueToInvalidCases() throws PromatServiceConnectorException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(500));
        ServiceErrorDto err = response.readEntity(ServiceErrorDto.class);
        assertThat("is errorcode", err.getCode(), is(ServiceErrorCode.FAILED));
        assertThat("status error cause", err.getCause().equals("Case ready for payment has not-approved tasks"));
        assertThat("status error details", err.getDetails().equals("Case id 1150 task(s) that has not been approved"));

        PromatCase fetched = promatServiceConnector.getCase(1150);
        fetched.setStatus(CaseStatus.CLOSED);
        promatServiceConnector.updateCase(1150, new CaseRequest(fetched));

        // Still has invalid cases (cases with unapproved tasks)
        response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(500));
        err = response.readEntity(ServiceErrorDto.class);
        assertThat("is errorcode", err.getCode(), is(ServiceErrorCode.FAILED));
        assertThat("status error cause", err.getCause().equals("Case ready for payment has no reviewer assigned"));
        assertThat("status error details", err.getDetails().equals("Case id 1160 has reviewer=null"));

        fetched = promatServiceConnector.getCase(1160);
        fetched.setStatus(CaseStatus.CLOSED);
        promatServiceConnector.updateCase(1160, new CaseRequest(fetched));

        // Now payments should be able to execute
        response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(200));
    }

    // This test needs to run after test that depends on the state of the preloaded cases
    @Order(4)
    @Test
    public void TestGetPaymentsAsCvs() {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(200));

        String csv = response.readEntity(String.class);
        assertThat("number of lines", csv.lines().count(), is(36L));  // 1 header + 34 paymentlines
    }

    // This test needs to run after test that depends on the state of the preloaded cases
    @Order(5)
    @Test
    public void TestGetPaymentsAsPaymentList() throws JsonProcessingException {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","PAYMENT_LIST"));
        assertThat("status code", response.getStatus(), is(200));

        PaymentList payments = mapper.readValue(response.readEntity(String.class), PaymentList.class);
        assertThat("number of paymentlines", payments.getPayments().size(), is(35));
    }

    // This test needs to run after test that depends on the state of the preloaded cases
    @Order(6)
    @Test
    public void TestPaymentFile() {
        Response response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(200));

        String csv = response.readEntity(String.class);
        assertThat("number of lines", csv.lines().count(), is(36L));  // 1 header + 35 paymentlines

        verifyPaymentCsv(csv);
    }

    // This test needs to run after test that checks the pending-payments list
    @Order(7)
    @Test
    public void TestExecutePayment() throws PromatServiceConnectorException {

        // Pay all pending payments
        Response response = putResponse("v1/api/payments/execute", "");
        assertThat("status code", response.getStatus(), is(200));

        String csv = response.readEntity(String.class);
        assertThat("number of lines", csv.lines().count(), is(36L));  // 1 header + 34 paymentlines

        // Now check that all pending payments has been payed
        response = getResponse("v1/api/payments/preview", Map.of("format","CSV"));
        assertThat("status code", response.getStatus(), is(200));

        csv = response.readEntity(String.class);
        assertThat("number of line", csv.lines().count(), is(1L));  // 1 header

        // And finally, check that case 1060 has been changed from status PENDING_CLOSE to CLOSED
        PromatCase fetched = promatServiceConnector.getCase(1060);
        assertThat("case has status CLOSED", fetched.getStatus(), is(CaseStatus.CLOSED));
    }

    @Order(8)
    @Test
    public void TestGetPreviousPayment() throws JsonProcessingException {

        Response response = getResponse("v1/api/payments/history");
        assertThat("status code", response.getStatus(), is(200));

        // Note: Case #20 is defined in promatcases.sql to test html caseview
        List<String> history = response.readEntity(List.class);
        assertThat("number of history lines is", history.size(), is(4));

        // Make sure we have two old payments, in the expected order (oldest->newest)
        assertThat("first old payment", history.get(0).equals("20201210_000000000"));
        assertThat("second old payment", history.get(1).equals("20201221_121314567"));

        // Get stamp of the latest payment and check that it originated at a time close to now
        String stamp = history.get(3);
        LocalDateTime latest = LocalDateTime.parse(stamp, DateTimeFormatter.ofPattern(Payments.TIMESTAMP_FORMAT));
        assertThat("is newest payment", latest.plusMinutes(1l), is(greaterThanOrEqualTo(LocalDateTime.now())));

        // Check that the previous payment returns exactly the same payment as when executed
        response = getResponse("v1/api/payments/preview", Map.of("format","CSV", "stamp", stamp));
        assertThat("status code", response.getStatus(), is(200));

        String csv = response.readEntity(String.class);
        assertThat("number of lines", csv.lines().count(), is(36L));  // 1 header + 35 paymentlines

        verifyPaymentCsv(csv);
    }

    private void verifyPaymentCsv(String csv) {
        LOGGER.info("Received CSV output is:\n{}", csv);

        // Make sure that the expected lines is sorted by the pay category (number)
        // ascending - this is how lines is output
        String expected = ("Dato;Lønnr.;Lønart;Antal;Tekst;Anmelder\n" +
                "mm-dd-åååå;123;1956;1;019997 Title for 019997;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;100007 Title for 100007;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;100008 Title for 100008;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;100009 Title for 100009;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;319997 Title for 319997;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001000 Case 1;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001010 Case 2;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001020 Case 3;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001030 Case 4;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001040 Case 5;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001050 Case 6;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001060 Case 7;Hans Hansen\n" +
                "mm-dd-åååå;123;1962;1;1001060 Bkm Case 7;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001070 Case 8;Hans Hansen\n" +
                "mm-dd-åååå;123;1962;1;1001070 Bkm Case 8;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001080,1001081,1001082,1001083 Case 9;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;2;1001080,1001081,1001082,1001083 Kort om, +2 Case 9;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001090,1001091,1001092,1001093,1001094 Case 10;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;2;1001090,1001091,1001092,1001093,1001094 Kort om, +2 Case 10;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001100,1001101,1001102,1001103,1001104 Case 11;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;3;1001100,1001101,1001102,1001103,1001104 Kort om, +3 Case 11;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;1001110,1001111 Case 12;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001110,1001111 Kort om, +1 Case 12;Hans Hansen\n" +
                "mm-dd-åååå;123;1987;2;1001110,1001111 Metadata Case 12;Hans Hansen\n" +
                "mm-dd-åååå;456;1981;1;1001130,1001131 Case 14;Ole Olsen\n" +
                "mm-dd-åååå;456;1960;1;1001140,1001141,1001142 Kort om, +1 Case 15;Ole Olsen\n" +
                "mm-dd-åååå;456;1954;1;1001170,1001171,1001172 Case 18;Ole Olsen\n" +
                "mm-dd-åååå;456;1960;1;1001170,1001171,1001172 Kort om, +1 Case 18;Ole Olsen\n" +
                "mm-dd-åååå;456;1954;1;1001180,1001181,1001182 Case 19;Ole Olsen\n" +
                "mm-dd-åååå;456;1960;1;1001180,1001181,1001182 Kort om, +1 Case 19;Ole Olsen\n" +
                "mm-dd-åååå;123;1956;1;1001190,1001191 Case 20;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;1001190,1001191 Kort om, +1 Case 20;Hans Hansen\n" +
                "mm-dd-åååå;123;1956;1;38529633,38529668 Case 20;Hans Hansen\n" +
                "mm-dd-åååå;123;1960;1;38529633,38529668 Kort om, +1 Case 20;Hans Hansen\n" +
                "mm-dd-åååå;123;1988;1;38529633,38529668 Case 20;Hans Hansen\n")
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
}
