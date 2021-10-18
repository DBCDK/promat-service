/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.ReviewerView;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.SubjectNote;
import dk.dbc.promat.service.templating.Formatting;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@TestMethodOrder(OrderAnnotation.class)
public class ReviewersIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewersIT.class);

    @Test
    void createReviewer() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("2407776666")
                .withPaycode(6666)
                .withFirstName("John")
                .withLastName("Doe")
                .withEmail("john@doe.com");

        final Response response = postResponse("v1/api/reviewers", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(201));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("reviewer entity", reviewer.getCulrId(), is("8ed780d6-46eb-4706-a4dc-a59f412d16c0"));

        // Example log entry:
        // [docker-java-stream-2022862423] INFO dk.dbc.promat.service.ContainerTest - STDOUT: {"timestamp":"2021-08-04T23:41:34.661920+02:00","sys_event_type":"audit","client_ip":["172.17.0.1"],"app_name":"PROMAT","action":"CREATE","accessing_user":{"token":"1-2-3-4-5"},"owning_user":"6666/190976","PROMAT":{"Created new user":"reviewers","Response":"200"}}
        assertThat("auditlog entry", promatServiceContainer.getLogs().contains("{\"Created new user\":\"reviewers\",\"Response\":\"201\"}"));
    }

    @Test
    void createReviewerWithoutCprNumber() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest().withPaycode(42);

        final Response response = postResponse("v1/api/reviewers", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReviewerWithoutPaycode() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest().withCprNumber("1234567890");

        final Response response = postResponse("v1/api/reviewers", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReviewerWithNonExistingSubject() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("2407776666")
                .withPaycode(6666)
                .withSubjects(List.of(4242));

        final Response response = postResponse("v1/api/reviewers", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReviewerWithoutNonNullField() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("2407776666")
                .withPaycode(6666)
                .withFirstName("John")
                .withLastName("Doe")
                .withAccepts(Collections.emptyList());
                // missing non-null email

        final Response response = postResponse("v1/api/reviewers", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void getReviewerWithoutAuthToken() throws JsonProcessingException {
        final Response response = getResponse("v1/api/reviewers/1");
        assertThat("response status", response.getStatus(), is(401));

        // Log contains no messages warning that no token was given when required
    }

    @Test
    void getReviewerWithInactiveAuthToken() throws JsonProcessingException {
        final Response response = getResponse("v1/api/reviewers/1", "6-7-8-9-0");
        assertThat("response status", response.getStatus(), is(401));

        // Example log entry:
        // [docker-java-stream--208115991] INFO dk.dbc.promat.service.ContainerTest - STDOUT: 14:14:47.700 [INFO] [http-thread-pool::http-listener(18)] dk.dbc.commons.rs.auth.DBCAuthenticationMechanism - Token is invalid: 6-7-8-9-0
        assertThat("rs-auth log entry", promatServiceContainer.getLogs().contains("Token is invalid: 6-7-8-9-0"));
    }

    @Test
    void getReviewer() throws JsonProcessingException, NoSuchAlgorithmException, KeyManagementException {
        final Reviewer expectedReviewer = new Reviewer();
        loadReviewer1(expectedReviewer, ReviewerView.Reviewer.class);

        final Response response = getResponse("v1/api/reviewers/1", "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));

        assertThat("reviewer", reviewer, is(expectedReviewer));

        // Example log entry:
        // [docker-java-stream--208115991] INFO dk.dbc.promat.service.ContainerTest - STDOUT: {"timestamp":"2021-08-04T14:14:48.151178+02:00","sys_event_type":"audit","client_ip":["172.17.0.1"],"app_name":"PROMAT","action":"READ","accessing_user":{"token":"1-2-3-4-5"},"owning_user":"123/190976","PROMAT":{"View full profile":"reviewers/1","Response":"200"}}
        assertThat("auditlog entry", promatServiceContainer.getLogs().contains("{\"View full profile\":\"reviewers/1\",\"Response\":\"200\"}"));
    }

    @Test
    void reviewerNotFound() {
        final Response response = getResponse("v1/api/reviewers/4242", "1-2-3-4-5");
        assertThat(response.getStatus(), is(404));

        // Example log entry:
        // [docker-java-stream--208115991] INFO dk.dbc.promat.service.ContainerTest - STDOUT: {"timestamp":"2021-08-04T14:14:47.633206+02:00","sys_event_type":"audit","client_ip":["172.17.0.1"],"app_name":"PROMAT","action":"READ","accessing_user":{"token":"1-2-3-4-5"},"owning_user":"0/190976","PROMAT":{"Request for full profile":"reviewers/4242","Response":"404"}}
        assertThat("auditlog entry", promatServiceContainer.getLogs().contains("{\"Request for full profile\":\"reviewers/4242\",\"Response\":\"404\"}"));
    }

    @Test
    @Order(1)
    public void listReviewers() throws JsonProcessingException {
        final Reviewer reviewer1 = new Reviewer();
        loadReviewer1(reviewer1, ReviewerView.Summary.class);

        final Reviewer reviewer2 = new Reviewer();
        loadReviewer2(reviewer2, ReviewerView.Summary.class);

        final Reviewer reviewer3 = new Reviewer();
        loadReviewer3(reviewer3, ReviewerView.Summary.class);

        final Reviewer reviewer4 = new Reviewer();
        loadReviewer4(reviewer4, ReviewerView.Summary.class);

        final Reviewer reviewer5 = new Reviewer();
        loadReviewer5(reviewer5, ReviewerView.Summary.class);

        final Reviewer reviewer6 = new Reviewer();
        loadReviewer6(reviewer6, ReviewerView.Summary.class);

        final Reviewer reviewer7 = new Reviewer();
        loadReviewer7(reviewer7, ReviewerView.Summary.class);

        final Reviewer reviewer8 = new Reviewer();
        loadReviewer8(reviewer8, ReviewerView.Summary.class);

        final Reviewer reviewer9 = new Reviewer();
        loadReviewer9(reviewer9, ReviewerView.Summary.class);

        final Reviewer reviewer15 = new Reviewer();
        loadReviewer15(reviewer15, ReviewerView.Summary.class);

        final ReviewerList<Reviewer> expected = new ReviewerList<>()
                .withReviewers(List.of(reviewer1, reviewer2, reviewer3, reviewer4, reviewer5, reviewer6, reviewer7,
                        reviewer8, reviewer9, reviewer15));

        final Response response = getResponse("v1/api/reviewers");

        final ReviewerList<Reviewer> actual = mapper.readValue(
                response.readEntity(String.class), new TypeReference<>() {});

        // The activeChanged stamp may differ on some reviewers since they have been updated by
        // other tests, so we need to reset the stamp to a known value before comparing with
        // hardcoded expected results
        for( Reviewer reviewer: actual.getReviewers() ) {
            reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
            reviewer.setDeactivated(null);
        }

        assertThat("List of reviewers has all reviewers",
                actual, is(expected));
    }

    @Test
    @Order(1)
    public void listReviewersWithWorkloads() throws JsonProcessingException {
        final ReviewerWithWorkloads reviewer1 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(1);
        loadReviewer1(reviewer1, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer2 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer2(reviewer2, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer3 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer3(reviewer3, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer4 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer4(reviewer4, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer5 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer5(reviewer5, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer6 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer6(reviewer6, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer7 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer7(reviewer7, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer8 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer8(reviewer8, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer9 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer9(reviewer9, ReviewerView.Summary.class);

        final ReviewerWithWorkloads reviewer15 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer15(reviewer15, ReviewerView.Summary.class);

        final ReviewerList<ReviewerWithWorkloads> expected = new ReviewerList<ReviewerWithWorkloads>()
                .withReviewers(List.of(reviewer1, reviewer2, reviewer3, reviewer4, reviewer5, reviewer6, reviewer7,
                        reviewer8, reviewer9, reviewer15));

        final Response response = getResponse("v1/api/reviewers",
                Map.of("deadline", "2020-12-01"));

        final ReviewerList<ReviewerWithWorkloads> actual = mapper.readValue(
                response.readEntity(String.class), new TypeReference<>() {});

        // The activeChanged stamp may differ on some reviewers since they have been updated by
        // other tests, so we need to reset the stamp to a known value before comparing with
        // hardcoded expected results
        for( Reviewer reviewer: actual.getReviewers() ) {
            reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
            reviewer.setDeactivated(null);
        }

        assertThat("List of reviewers is just 'Hans Hansen', 'Ole Olsen', 'Peter Petersen', 'kirsten kirstensen' and 'Boe Boesen'",
                actual, is(expected));
    }

    @Test
    void updateReviewer() throws JsonProcessingException {

        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withActive(false)
                .withAccepts(Arrays.asList(Reviewer.Accepts.BOOK, Reviewer.Accepts.MULTIMEDIA))
                .withAddress(new Address().withAddress1("Mellemgade 51").withAddress2("Øvre Mellem").withCity("Mellemtved").withZip("6666").withSelected(true))
                .withPrivateAddress(new Address().withAddress1("Hjemmegade 51").withAddress2("Hjemme").withCity("Hjemmeby").withZip("1236").withSelected(false))
                .withEmail("peder@pedersen.dk")
                .withPrivateEmail("peder-pedersen@hjemme.dk")
                .withFirstName("Peder")
                .withLastName("Pedersen")
                .withHiatusBegin(LocalDate.parse("2020-12-22"))
                .withHiatusEnd(LocalDate.parse("2021-01-03"))
                .withInstitution("Peder Pedersens pedaler")
                .withPaycode(7777)
                .withPhone("87654321")
                .withPrivatePhone("12345678")
                .withSubjects(List.of(3))
                .withCprNumber("123456-7890");  // Should not be used and not cause any conflict

        final Response response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        final Reviewer updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        updated.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        updated.setDeactivated(null);

        final Reviewer expected = new Reviewer();
        loadUpdatedReviewer3(expected, ReviewerView.Reviewer.class);

        assertThat("Reviewer has been updated", updated, is(expected));
        assertThat("Equals", updated.equals(expected));

        // Example log entries:
        //[docker-java-stream-1861590606] INFO dk.dbc.promat.service.ContainerTest - STDOUT: {"timestamp":"2021-08-04T23:19:49.611132+02:00","sys_event_type":"audit","client_ip":["172.17.0.1"],"app_name":"PROMAT","action":"UPDATE","accessing_user":{"token":"1-2-3-4-5"},"owning_user":"22/190976","PROMAT":{"Change of paycode (owning id)":"reviewers/3","Current value":"22","New value":"7777"}}
        //[docker-java-stream-1861590606] INFO dk.dbc.promat.service.ContainerTest - STDOUT: {"timestamp":"2021-08-04T23:19:49.614198+02:00","sys_event_type":"audit","client_ip":["172.17.0.1"],"app_name":"PROMAT","action":"UPDATE","accessing_user":{"token":"1-2-3-4-5"},"owning_user":"7777/190976","PROMAT":{"Update and view full profile":"reviewers/3","Response":"200"}}
        assertThat("auditlog paycode change", promatServiceContainer.getLogs().contains("{\"Change of paycode (owning id)\":\"reviewers/3\",\"Current value\":\"22\",\"New value\":\"7777\"}"));
        assertThat("auditlog update", promatServiceContainer.getLogs().contains("{\"Update and view full profile\":\"reviewers/3\",\"Response\":\"200\"}"));
    }

    @Test
    void updateSelectedAddress() throws JsonProcessingException {

        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withActive(false)
                .withAccepts(Arrays.asList(Reviewer.Accepts.BOOK, Reviewer.Accepts.MULTIMEDIA))
                .withAddress(new Address().withAddress1("Mellemgade 51").withAddress2("Øvre Mellem").withCity("Mellemtved").withZip("6666").withSelected(null))
                .withPrivateAddress(new Address().withAddress1("Hjemmegade 51").withAddress2("Hjemme").withCity("Hjemmeby").withZip("1236").withSelected(null))
                .withEmail("peder@pedersen.dk")
                .withPrivateEmail("peder-pedersen@hjemme.dk")
                .withFirstName("Peder")
                .withLastName("Pedersen")
                .withHiatusBegin(LocalDate.parse("2020-12-22"))
                .withHiatusEnd(LocalDate.parse("2021-01-03"))
                .withInstitution("Peder Pedersens pedaler")
                .withPaycode(7777)
                .withPhone("87654321")
                .withPrivatePhone("12345678")
                .withSubjects(List.of(3))
                .withCprNumber("123456-7890");  // Should not be used and not cause any conflict

        Response response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        Reviewer updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is not selected", updated.getAddress().getSelected(), is(true));
        assertThat("private address is not selected", updated.getPrivateAddress().getSelected(), is(false));

        reviewerRequest.getAddress().setSelected(false);
        reviewerRequest.getPrivateAddress().setSelected(false);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is selected", updated.getAddress().getSelected(), is(true));
        assertThat("private address is not selected", updated.getPrivateAddress().getSelected(), is(false));

        reviewerRequest.getAddress().setSelected(true);
        reviewerRequest.getPrivateAddress().setSelected(null);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is selected", updated.getAddress().getSelected(), is(true));
        assertThat("private address is not selected", updated.getPrivateAddress().getSelected(), is(false));

        reviewerRequest.getAddress().setSelected(true);
        reviewerRequest.getPrivateAddress().setSelected(false);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is selected", updated.getAddress().getSelected(), is(true));
        assertThat("private address is not selected", updated.getPrivateAddress().getSelected(), is(false));

        reviewerRequest.getAddress().setSelected(true);
        reviewerRequest.getPrivateAddress().setSelected(true);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address selected is prioritized above private address selected", updated.getAddress().getSelected(), is(true));
        assertThat("private address selected is not prioritized above work address selected", updated.getPrivateAddress().getSelected(), is(false));

        reviewerRequest.getAddress().setSelected(false);
        reviewerRequest.getPrivateAddress().setSelected(true);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is not selected", updated.getAddress().getSelected(), is(false));
        assertThat("Private address is selected", updated.getPrivateAddress().getSelected(), is(true));

        reviewerRequest.getAddress().setSelected(null);
        reviewerRequest.getPrivateAddress().setSelected(true);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is not selected", updated.getAddress().getSelected(), is(false));
        assertThat("Private address is selected", updated.getPrivateAddress().getSelected(), is(true));

        reviewerRequest.getAddress().setSelected(null);
        reviewerRequest.getPrivateAddress().setSelected(false);
        response = putResponse("v1/api/reviewers/3", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("work address is selected", updated.getAddress().getSelected(), is(true));
        assertThat("Private address is not selected", updated.getPrivateAddress().getSelected(), is(false));
    }

    private void loadReviewer1(Reviewer reviewer, Class view) {
        reviewer.setId(1);
        reviewer.setActive(true);
        reviewer.setFirstName("Hans");
        reviewer.setLastName("Hansen");
        reviewer.setPaycode(123);
        reviewer.setInstitution("Hans Hansens Bix");
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(3)
                                .withName("Eventyr, fantasy")
                                .withParentId(2),
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer.setHiatusBegin(LocalDate.parse("2020-10-28"));
        reviewer.setHiatusEnd(LocalDate.parse("2020-11-01"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
        reviewer.setNote("note1");
        reviewer.setCapacity(1);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class ) {
            reviewer.setCulrId("41");
            reviewer.setEmail("hans@hansen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Lillegade 1")
                            .withZip("9999")
                            .withCity("Lilleved")
                            .withSelected(true));
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer2(Reviewer reviewer, Class view) {
        reviewer.setId(2);
        reviewer.setActive(true);
        reviewer.setFirstName("Ole");
        reviewer.setLastName("Olsen");
        reviewer.setInstitution("Ole Olsens Goodies");
        reviewer.setPaycode(456);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer.setHiatusBegin(LocalDate.parse("2020-11-28"));
        reviewer.setHiatusEnd(LocalDate.parse("2020-12-01"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
        reviewer.setNote("note2");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class) {
            reviewer.setCulrId("42");
            reviewer.setEmail("ole@olsen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Storegade 99")
                            .withZip("1111")
                            .withCity("Storeved")
                            .withSelected(true));
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer3(Reviewer reviewer, Class view) {
        reviewer.setId(3);
        reviewer.setActive(true);
        reviewer.setFirstName("Peter");
        reviewer.setLastName("Petersen");
        reviewer.setInstitution("Peter Petersens pedaler");
        reviewer.setPaycode(22);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer.setHiatusBegin(null);
        reviewer.setHiatusEnd(null);
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK));
        reviewer.setNote("note3");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class) {
            reviewer.setCulrId("43");
            reviewer.setEmail("peter@petersen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Mellemgade 50")
                            .withZip("5555")
                            .withCity("Mellemved")
                            .withSelected(true));
            reviewer.setPhone("12345678");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer4(Reviewer reviewer, Class view) {
        reviewer.setId(4);
        reviewer.setActive(true);
        reviewer.setFirstName("Kirsten");
        reviewer.setLastName("Kirstensen");
        reviewer.setInstitution("Kirstens Bix");
        reviewer.setPaycode(0);
        reviewer.setHiatusBegin(LocalDate.parse("2021-01-11"));
        reviewer.setHiatusEnd(LocalDate.parse("2021-01-12"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK));
        reviewer.setSubjects(List.of(
                new Subject()
                        .withId(5)
                        .withName("Multimedie")));
        reviewer.setNote("note4");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class) {
            reviewer.setCulrId("55");
            reviewer.setEmail("kirsten@kirstensen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Overgade 50")
                            .withZip("5432")
                            .withCity("Overlev")
                            .withSelected(true));
            reviewer.setPhone("123456789010");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer5(Reviewer reviewer, Class view) {
        reviewer.setId(5);
        reviewer.setActive(true);
        reviewer.setFirstName("Boe");
        reviewer.setLastName("Boesen");
        reviewer.setInstitution("Boe Boesens Bøjler");
        reviewer.setPaycode(23);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer.setHiatusBegin(null);
        reviewer.setHiatusEnd(null);
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK));
        reviewer.setNote("note5");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class ) {
            reviewer.setCulrId("44");
            reviewer.setEmail("boe@boesen.dk");
            reviewer.setPhone("9123456789");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadUpdatedReviewer3(Reviewer reviewer, Class view) {
        reviewer.setId(3);
        reviewer.setActive(false);
        reviewer.setFirstName("Peder");
        reviewer.setLastName("Pedersen");
        reviewer.setInstitution("Peder Pedersens pedaler");
        reviewer.setPaycode(7777);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(3)
                                .withName("Eventyr, fantasy")
                                .withParentId(2)));
        reviewer.setHiatusBegin(LocalDate.parse("2020-12-22"));
        reviewer.setHiatusEnd(LocalDate.parse("2021-01-03"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK, Reviewer.Accepts.MULTIMEDIA));
        reviewer.setNote("note3");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class ) {
            reviewer.setCulrId("43");
            reviewer.setEmail("peder@pedersen.dk");
            reviewer.setPrivateEmail("peder-pedersen@hjemme.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Mellemgade 51")
                            .withZip("6666")
                            .withCity("Mellemtved")
                            .withAddress2("Øvre Mellem")
                            .withSelected(true));
            reviewer.setPrivateAddress(
                    new Address()
                            .withAddress1("Hjemmegade 51")
                            .withZip("1236")
                            .withCity("Hjemmeby")
                            .withAddress2("Hjemme")
                            .withSelected(false));
            reviewer.setPhone("87654321");
            reviewer.setPrivatePhone("12345678");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer6(Reviewer reviewer, Class view) {
        reviewer.setId(6);
        reviewer.setActive(true);
        reviewer.setFirstName("Michael");
        reviewer.setLastName("Michelsen");
        reviewer.setInstitution("Michs Mechanics");
        reviewer.setPaycode(232221);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(5)
                                .withName("Multimedie"),
                        new Subject()
                                .withId(6).withParentId(5).withName("(Et Mulitmedie underemne)")));
        reviewer.setHiatusBegin(null);
        reviewer.setHiatusEnd(null);
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA));
        reviewer.setNote("note6");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of(
                new SubjectNote().withNote("Anmelder det meste").withId(1).withSubjectId(5),
                new SubjectNote().withNote("(Hvis et eller andet underemne, så kommentar)").withId(2).withSubjectId(6)
        ));
        if( view == ReviewerView.Reviewer.class ) {
            reviewer.setCulrId("56434241");
            reviewer.setEmail("mich@mich.dk");
            reviewer.setAddress(new Address().withSelected(true));
            reviewer.setPhone("912345678901");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer7(Reviewer reviewer, Class view) {
        reviewer.setId(7);
        reviewer.setActive(false);
        reviewer.setFirstName("Holger");
        reviewer.setLastName("Holgersen");
        reviewer.setInstitution("Holgers Holdings");
        reviewer.setPaycode(232222);
        reviewer.setHiatusBegin(null);
        reviewer.setHiatusEnd(null);
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA));
        reviewer.setNote("note7");
        reviewer.setCapacity(2);
        reviewer.setSubjects(List.of());
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class ) {
            reviewer.setCulrId("56434242");
            reviewer.setEmail("holg@holg.dk");
            reviewer.setPrivateEmail("holger.privat@holg.dk");
            reviewer.setAddress(new Address().withSelected(true));
            reviewer.setPhone("912345678902");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer8(Reviewer reviewer, Class view) {
        reviewer.setId(8);
        reviewer.setActive(true);
        reviewer.setFirstName("Søren");
        reviewer.setLastName("Sørensen");
        reviewer.setInstitution("Sørens far har penge");
        reviewer.setPaycode(0);
        reviewer.setHiatusBegin(LocalDate.parse("2021-01-11"));
        reviewer.setHiatusEnd(LocalDate.parse("2021-01-12"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK));
        reviewer.setSubjects(List.of(
                new Subject()
                        .withId(5)
                        .withName("Multimedie")));
        reviewer.setNote("note4");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class) {
            reviewer.setCulrId("88");
            reviewer.setEmail("soren@sorensen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Sørenstræde 7")
                            .withAddress2("Sørensby")
                            .withZip("5555")
                            .withCity("Sørlev")
                            .withSelected(true));
            reviewer.setPhone("11223344");
            reviewer.setPrivateEmail("sorenprivat@sorensen.dk");
            reviewer.setPrivateAddress(
                    new Address()
                            .withAddress1("Olesggade 7")
                            .withAddress2("Olestrup")
                            .withZip("5432")
                            .withCity("Olesby")
                            .withSelected(false));
            reviewer.setPrivatePhone("98653274");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    private void loadReviewer9(Reviewer reviewer, Class view) {
        reviewer.setId(9);
        reviewer.setActive(false);
        reviewer.setFirstName("Søren");
        reviewer.setLastName("Sørensen");
        reviewer.setInstitution("Sørens far har penge");
        reviewer.setPaycode(0);
        reviewer.setHiatusBegin(LocalDate.parse("2021-01-11"));
        reviewer.setHiatusEnd(LocalDate.parse("2021-01-12"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK));
        reviewer.setSubjects(List.of(
                new Subject()
                        .withId(5)
                        .withName("Multimedie")));
        reviewer.setNote("note4");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class) {
            reviewer.setCulrId("99");
            reviewer.setEmail("soren@sorensen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Sørenstræde 7")
                            .withAddress2("Sørensby")
                            .withZip("5555")
                            .withCity("Sørlev")
                            .withSelected(true));
            reviewer.setPhone("11223344");
            reviewer.setPrivateEmail("sorenprivat@sorensen.dk");
            reviewer.setPrivateAddress(
                    new Address()
                            .withAddress1("Olesggade 7")
                            .withAddress2("Olestrup")
                            .withZip("5432")
                            .withCity("Olesby")
                            .withSelected(false));
            reviewer.setPrivatePhone("98653274");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }
    private void loadReviewer15(Reviewer reviewer, Class view) {
        reviewer.setId(15);
        reviewer.setActive(false);
        reviewer.setFirstName("Søren");
        reviewer.setLastName("Sørensen");
        reviewer.setInstitution("Sørens far har penge");
        reviewer.setPaycode(0);
        reviewer.setHiatusBegin(LocalDate.parse("2021-01-11"));
        reviewer.setHiatusEnd(LocalDate.parse("2021-01-12"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.BOOK));
        reviewer.setSubjects(List.of(
                new Subject()
                        .withId(5)
                        .withName("Multimedie")));
        reviewer.setNote("note4");
        reviewer.setCapacity(2);
        reviewer.setSubjectNotes(List.of());
        if( view == ReviewerView.Reviewer.class) {
            reviewer.setCulrId("1515");
            reviewer.setEmail("soren@sorensen.dk");
            reviewer.setAddress(
                    new Address()
                            .withAddress1("Sørenstræde 7")
                            .withAddress2("Sørensby")
                            .withZip("5555")
                            .withCity("Sørlev")
                            .withSelected(true));
            reviewer.setPhone("11223344");
            reviewer.setPrivateEmail("sorenprivat@sorensen.dk");
            reviewer.setPrivateAddress(
                    new Address()
                            .withAddress1("Olesggade 7")
                            .withAddress2("Olestrup")
                            .withZip("5432")
                            .withCity("Olesby")
                            .withSelected(false));
            reviewer.setPrivatePhone("98653274");
        }
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
    }

    @Test
    public void testReviewerFormat() {
        Reviewer reviewer = new Reviewer();
        String actual = Formatting.format(reviewer);
        assertThat("name is correct formatted", actual.equals(""));

        reviewer = new Reviewer()
                .withFirstName("Hans Ole Erik");
        actual = Formatting.format(reviewer);
        assertThat("name is correct formatted", actual.equals("Hans Ole Erik"));

        reviewer = new Reviewer()
                .withLastName("Petersen Sørensen");
        actual = Formatting.format(reviewer);
        assertThat("name is correct formatted", actual.equals("Petersen Sørensen"));

        reviewer = new Reviewer()
                .withFirstName("Hans Ole Erik")
                .withLastName("Petersen Sørensen");
        actual = Formatting.format(reviewer);
        assertThat("name is correct formatted", actual.equals("Hans Ole Erik Petersen Sørensen"));

        reviewer = new Reviewer()
                .withFirstName("Hans")
                .withLastName("Hansen");
        actual = Formatting.format(reviewer);
        assertThat("name is correct formatted", actual.equals("Hans Hansen"));
    }

    @Test
    public void testUpdateReviewerWithNullAddress() throws JsonProcessingException {

        Response response = getResponse("v1/api/reviewers/5", "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("address", reviewer.getAddress(), is(nullValue()));

        final ReviewerRequest reviewerUpdateRequest = new ReviewerRequest()
            .withAddress(new Address().withAddress1("Boesvej 1"));
        response = putResponse("v1/api/reviewers/" + reviewer.getId(), reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateReviewerWithCapacity() throws JsonProcessingException {

        final ReviewerRequest reviewerUpdateRequest = new ReviewerRequest()
                .withCapacity(5);
        Response response = putResponse("v1/api/reviewers/5", reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("capacity", reviewer.getCapacity(), is(5));
    }

    @Test
    public void testUpdateReviewerWithNote() throws JsonProcessingException {

        final ReviewerRequest reviewerUpdateRequest = new ReviewerRequest()
                .withNote("newnote");
        Response response = putResponse("v1/api/reviewers/5", reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("note", reviewer.getNote(), is("newnote"));
    }

    @Test
    public void testUpdateSubjectNotes() throws JsonProcessingException {
        final ReviewerRequest reviewerUpdateRequest = new ReviewerRequest();
        Response response = getResponse("v1/api/reviewers/6", "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        Reviewer fetched = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        Reviewer reviewer6 = new Reviewer();
        loadReviewer6(reviewer6, ReviewerView.Reviewer.class);
        fetched.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        fetched.setDeactivated(null);
        assertThat(fetched, is(reviewer6));

        // Check that notes can be changed.
        List<Subject> subjects = List.of(
          new Subject().withId(1).withName("Voksen"),
          new Subject().withId(2).withName("Roman").withParentId(1));
        List<Integer> subjectIds = List.of(1, 2);
        List<SubjectNote> notes = List.of(
                new SubjectNote().withSubjectId(1).withNote("Some note to subject 1: ('Voksen')"),
                new SubjectNote().withSubjectId(2).withNote("Some other note to subject 2: ('Roman')"));

        reviewerUpdateRequest.withSubjects(subjectIds).setSubjectNotes(notes);
        response = putResponse("v1/api/reviewers/6", reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        fetched = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        fetched.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        fetched.setDeactivated(null);
        reviewer6.withSubjects(subjects).withSubjectNotes(notes);
        assertThat(fetched, is(reviewer6));

        // Check that there is no way that a note can be made to a subject not
        // associated with this reviewer.
        reviewerUpdateRequest.withSubjectNotes(
                List.of(new SubjectNote().withSubjectId(4).withNote("Some note")));
        response = putResponse("v1/api/reviewers/6", reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(400));


        // Check that if we delete a subject (1) that has a related note, this note is also removed.
        reviewerUpdateRequest.withSubjectNotes(null).withSubjects(List.of(2));
        response = putResponse("v1/api/reviewers/6", reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("resonse status", response.getStatus(), is(200));
        fetched = mapper.readValue(response.readEntity(String.class), Reviewer.class);

        assertThat("no 'Some note to subject 1'", fetched.getSubjectNotes()
                .stream()
                .noneMatch(subjectNote -> subjectNote.getNote().contains("Some note to subject 1")));


        // Put back initial subjects and subjectNotes, as to not wreck later tests.
        reviewerUpdateRequest
                .withSubjects(List.of(5, 6))
                .withSubjectNotes(
                        List.of(
                            new SubjectNote().withSubjectId(5).withNote("Anmelder det meste"),
                            new SubjectNote().withSubjectId(6).withNote("(Hvis et eller andet underemne, så kommentar)")));
        response = putResponse("v1/api/reviewers/6", reviewerUpdateRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        fetched = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        fetched.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        fetched.setDeactivated(null);
        loadReviewer6(reviewer6, ReviewerView.Reviewer.class);
        assertThat(fetched, is(reviewer6));
    }

    @Test
    public void testThatMailsAreOnlySentOnRealChangesAndWhenNotifyQueryParmIsTrue() {

        // Add private address, And check that a mail is added to the mailqueue with the changes.
        ReviewerRequest reviewerRequest =
                new ReviewerRequest()
                        .withPrivateAddress(new Address().withAddress1("Hellig Helges Vej"));
        Response response = putResponse("v1/api/reviewers/7", reviewerRequest, Map.of("notify", true), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        List<Notification> notifications = getNotifications(null, "Hellig Helges Vej");

        assertThat("There is only one mail containing info with this address change",
                notifications.size(), is(1));
        assertThat("This is a mail to the LU mailaddress", notifications.get(0).getToAddress(), is("TEST@dbc.dk"));


        // Change the private address. Now with no notify parm. Expect nothing further in mail queue.
        reviewerRequest.getPrivateAddress().setAddress1("Thors Torden gade 11");
        response = putResponse("v1/api/reviewers/7", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        notifications = getNotifications(null, "Thors Torden");
        assertThat("There are no mails to this change", notifications.size(), is(0));

        // Now submit the same address, but now with notify. Expect no mail.
        response = putResponse("v1/api/reviewers/7", reviewerRequest, Map.of("notify", true), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        notifications = getNotifications(null, "Thors Torden");
        assertThat("There are no mails to this change", notifications.size(), is(0));

        // Change various stuff: Phone, private address, and "selected" on addresses.
        reviewerRequest = new ReviewerRequest()
                .withPhone("123456112233445566")
                .withPrivatePhone("987654321112233445566")
                .withPrivateAddress(new Address().withAddress1("Classensgade 12").withSelected(true))
                .withAddress(new Address().withSelected(false));
        response = putResponse("v1/api/reviewers/7", reviewerRequest, Map.of("notify", true), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        notifications = getNotifications(null, "123456112233445566");
        assertThat("There are one mail matching this change", notifications.size(), is(1));
        Notification notification = notifications.get(0);

        assertThat("Address change is present", notification.getBodyText()
                .contains("Classensgade 12"));

        assertThat("Private phone change is present", notification.getBodyText()
                .contains("987654321112233445566"));

        assertThat("Phone change is present", notification.getBodyText()
                .contains("123456112233445566"));

        assertThat("Private address is selected", notification.getBodyText()
                .contains("<tr>\n" +
                        "                <td>[privateSelected]</td>\n" +
                        "            </tr>\n" +
                        "            \n" +
                        "                <tr>\n" +
                        "                    <td><table>\n" +
                        "                            <tr>\n" +
                        "                                <td>\n" +
                        "                                    Fra:\n" +
                        "                                </td>\n" +
                        "                                <td>\n" +
                        "                                    false\n" +
                        "                                </td>\n" +
                        "                            </tr>\n" +
                        "                            <tr>\n" +
                        "                                <td>\n" +
                        "                                    Til:\n" +
                        "                                </td>\n" +
                        "                                <td>\n" +
                        "                                    true\n" +
                        "                                </td>\n" +
                        "                            </tr>\n" +
                        "                        </table>"));


    }

}
