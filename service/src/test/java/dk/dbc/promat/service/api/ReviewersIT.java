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
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.templating.Formatting;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
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

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(201));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("reviewer entity", reviewer.getCulrId(), is("8ed780d6-46eb-4706-a4dc-a59f412d16c0"));
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
                .withCprNumber("2407776666")
                .withPaycode(6666)
                .withSubjects(List.of(4242));

        final Response response = postResponse("v1/api/reviewers", reviewerRequest);
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

        final Reviewer reviewer3 = new Reviewer();
        loadReviewer3(reviewer3);

        final Reviewer reviewer4 = new Reviewer();
        loadReviewer4(reviewer4);


        final ReviewerList<Reviewer> expected = new ReviewerList<>()
                .withReviewers(List.of(reviewer1, reviewer2, reviewer3, reviewer4));

        final Response response = getResponse("v1/api/reviewers");

        final ReviewerList<Reviewer> actual = mapper.readValue(
                response.readEntity(String.class), new TypeReference<>() {});

        assertThat("List of reviewers is just 'Hans Hansen', 'Ole Olsen' and 'Peter Petersen'",
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

        final ReviewerWithWorkloads reviewer3 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer3(reviewer3);

        final ReviewerWithWorkloads reviewer4 = new ReviewerWithWorkloads()
                .withWeekWorkload(0)
                .withWeekBeforeWorkload(0)
                .withWeekAfterWorkload(0);
        loadReviewer4(reviewer4);

        final ReviewerList<ReviewerWithWorkloads> expected = new ReviewerList<ReviewerWithWorkloads>()
                .withReviewers(List.of(reviewer1, reviewer2, reviewer3, reviewer4));

        final Response response = getResponse("v1/api/reviewers",
                Map.of("deadline", "2020-12-01"));

        final ReviewerList<ReviewerWithWorkloads> actual = mapper.readValue(
                response.readEntity(String.class), new TypeReference<>() {});

        assertThat("List of reviewers is just 'Hans Hansen', 'Ole Olsen' and 'Peter Petersen' and 'kirsten kirstensen'",
                actual, is(expected));
    }

    @Test
    void updateReviewer() throws JsonProcessingException {

        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withActive(false)
                .withAccepts(Arrays.asList(Reviewer.Accepts.BOOK, Reviewer.Accepts.MULTIMEDIA))
                .withAddress(new Address().withAddress1("Mellemgade 51").withAddress2("Øvre Mellem").withCity("Mellemtved").withZip("6666"))
                .withEmail("peder@pedersen.dk")
                .withFirstName("Peder")
                .withLastName("Pedersen")
                .withHiatusBegin(LocalDate.parse("2020-12-22"))
                .withHiatusEnd(LocalDate.parse("2021-01-03"))
                .withInstitution("Peder Pedersens pedaler")
                .withPaycode(7777)
                .withPhone("87654321")
                .withSubjects(List.of(3))
                .withCprNumber("123456-7890");  // Should not be used and not cause any conflict

        final Response response = putResponse("v1/api/reviewers/3", reviewerRequest);
        assertThat("response status", response.getStatus(), is(200));
        final Reviewer updated = mapper.readValue(response.readEntity(String.class), Reviewer.class);

        final Reviewer expected = new Reviewer();
        loadUpdatedReviewer3(expected);

        assertThat("Reviewer has been updated", updated.equals(expected));
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
        reviewer.setPaycode(123);
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
    }

    private void loadReviewer3(Reviewer reviewer) {
        reviewer.setId(3);
        reviewer.setActive(true);
        reviewer.setCulrId("43");
        reviewer.setFirstName("Peter");
        reviewer.setLastName("Petersen");
        reviewer.setEmail("peter@petersen.dk");
        reviewer.setAddress(
                new Address()
                        .withAddress1("Mellemgade 50")
                        .withZip("5555")
                        .withCity("Mellemved"));
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
        reviewer.setPhone("12345678");
    }

    private void loadReviewer4(Reviewer reviewer) {
        reviewer.setId(4);
        reviewer.setActive(true);
        reviewer.setCulrId("55");
        reviewer.setFirstName("Kirsten");
        reviewer.setLastName("Kirstensen");
        reviewer.setEmail("kirsten@kirstensen.dk");
        reviewer.setAddress(
                new Address()
                        .withAddress1("Overgade 50")
                        .withZip("5432")
                        .withCity("Overlev"));
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
        reviewer.setNote("note5");
        reviewer.setPhone("123456789010");
    }

    private void loadUpdatedReviewer3(Reviewer reviewer) {
        reviewer.setId(3);
        reviewer.setActive(false);
        reviewer.setCulrId("43");
        reviewer.setFirstName("Peder");
        reviewer.setLastName("Pedersen");
        reviewer.setEmail("peder@pedersen.dk");
        reviewer.setAddress(
                new Address()
                        .withAddress1("Mellemgade 51")
                        .withZip("6666")
                        .withCity("Mellemtved")
                        .withAddress2("Øvre Mellem"));
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
        reviewer.setPhone("87654321");
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

        final ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withCprNumber("2407776666")
                .withPaycode(6666)
                .withFirstName("Bo")
                .withLastName("Boe")
                .withEmail("bo@boe.com");

        Response response = postResponse("v1/api/reviewers", reviewerRequest);
        assertThat("response status", response.getStatus(), is(201));

        final Reviewer reviewer = mapper.readValue(response.readEntity(String.class), Reviewer.class);
        assertThat("address", reviewer.getAddress(), is(nullValue()));

        final ReviewerRequest reviewerUpdateRequest = new ReviewerRequest()
            .withAddress(new Address().withAddress1("Boesvej 1"));
        response = putResponse("v1/api/reviewers/" + reviewer.getId(), reviewerUpdateRequest);
        assertThat("response status", response.getStatus(), is(200));
    }
}