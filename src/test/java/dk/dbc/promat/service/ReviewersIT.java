/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@TestMethodOrder(OrderAnnotation.class)
public class ReviewersIT extends ContainerTest {

    @Test
    void createReviewer() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("1234567890")
                .withPaycode(42)
                .withFirstName("John")
                .withLastName("Doe")
                .withEmail("john@doe.com");

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(201));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("reviewer entity", reviewer.getFirstName(), is("John"));
    }

    @Test
    void createReviewerWithoutCprNumber() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest().withPaycode(42);

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReviewerWithoutPaycode() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest().withCprNumber("1234567890");

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReviewerWithNonExistingSubject() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("1234567890")
                .withPaycode(42)
                .withSubjects(List.of(4242));

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReviewerWithoutNonNullField() throws JsonProcessingException {
        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("1234567890")
                .withPaycode(42)
                .withFirstName("John")
                .withLastName("Doe")
                .withAccepts(Collections.emptyList());
                // missing non-null email

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(400));

        final ServiceErrorDto serviceError = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("service error", serviceError.getCode(), is(ServiceErrorCode.INVALID_REQUEST));
    }

    @Test
    void getReviewer() throws JsonProcessingException {
        final Reviewer expectedReviewer = new Reviewer();
        loadReviewer1(expectedReviewer);

        final Response response = getResponse("v1/api/reviewers/1");
        assertThat("response status", response.getStatus(), is(200));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("reviewer", reviewer, is(expectedReviewer));
    }

    @Test
    void reviewerNotFound() {
        final Response response = getResponse("v1/api/reviewers/4242");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    @Order(1)
    public void listReviewers() throws JsonProcessingException {
        final Reviewer reviewer1 = new Reviewer();
        loadReviewer1(reviewer1);

        final Reviewer reviewer2 = new Reviewer();
        loadReviewer2(reviewer2);

        final ReviewerList<Reviewer> expected = new ReviewerList<>()
                .withReviewers(List.of(reviewer1, reviewer2));

        final Response response = getResponse("v1/api/reviewers");

        final ReviewerList<Reviewer> actual = mapper.readValue(
                response.readEntity(String.class), new TypeReference<>() {});

        assertThat("List of reviewers is just 'Hans Hansen' and 'Ole Olsen'",
                actual, is(expected));
    }

    @Test
    @Order(1)
    public void listReviewersWithWorkloads() throws JsonProcessingException {
        final ReviewerWithWorkloads reviewer1 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(1);
        loadReviewer1(reviewer1);

        final ReviewerWithWorkloads reviewer2 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer2(reviewer2);

        final ReviewerList<ReviewerWithWorkloads> expected = new ReviewerList<ReviewerWithWorkloads>()
                .withReviewers(List.of(reviewer1, reviewer2));

        final Response response = getResponse("v1/api/reviewers",
                Map.of("deadline", "2020-12-01"));

        final ReviewerList<ReviewerWithWorkloads> actual = mapper.readValue(
                response.readEntity(String.class), new TypeReference<>() {});

        assertThat("List of reviewers is just 'Hans Hansen' and 'Ole Olsen'",
                actual, is(expected));
    }

    private void loadReviewer1(Reviewer reviewer) {
        reviewer.setId(1);
        reviewer.setActive(true);
        reviewer.setCulrId("41");
        reviewer.setFirstName("Hans");
        reviewer.setLastName("Hansen");
        reviewer.setEmail("hans@hansen.dk");
        reviewer.setAddress(
                new Address()
                        .withAddress1("Lillegade 1")
                        .withZip("9999")
                        .withCity("Lilleved"));
        reviewer.setInstitution("Hans Hansens Bix");
        reviewer.setPaycode(0);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(3)
                                .withName("Eventyr, fantasy")
                                .withParentId(2),
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer.setHiatus_begin(LocalDate.parse("2020-10-28"));
        reviewer.setHiatus_end(LocalDate.parse("2020-11-01"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
    }

    private void loadReviewer2(Reviewer reviewer) {
        reviewer.setId(2);
        reviewer.setActive(true);
        reviewer.setCulrId("42");
        reviewer.setFirstName("Ole");
        reviewer.setLastName("Olsen");
        reviewer.setEmail("ole@olsen.dk");
        reviewer.setAddress(
                new Address()
                        .withAddress1("Storegade 99")
                        .withZip("1111")
                        .withCity("Storeved"));
        reviewer.setInstitution("Ole Olsens Goodies");
        reviewer.setPaycode(0);
        reviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer.setHiatus_begin(LocalDate.parse("2020-11-28"));
        reviewer.setHiatus_end(LocalDate.parse("2020-12-01"));
        reviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
    }
}
