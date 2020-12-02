/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ReviewersIT extends ContainerTest {

    @Test
    public void listReviewers() throws JsonProcessingException {
        final ReviewerList<Reviewer> expected = new ReviewerList<>().withReviewers(
                List.of(
                        new Reviewer()
                                .withId(1)
                                .withActive(true)
                                .withFirstName("Hans")
                                .withLastName("Hansen")
                                .withEmail("hans@hansen.dk")
                                .withAddress(
                                        new Address()
                                                .withAddress1("Lillegade 1")
                                                .withZip("9999")
                                                .withCity("Lilleved")

                                )
                                .withInstitution("Hans Hansens Bix")
                                .withPaycode(0)
                                .withSubjects(
                                        List.of(
                                                new Subject()
                                                        .withId(3)
                                                        .withName("Eventyr, fantasy")
                                                        .withParentId(2),
                                                new Subject()
                                                        .withId(5)
                                                        .withName("Multimedie")
                                        )
                                )
                                .withHiatus_begin(LocalDate.parse("2020-10-28"))
                                .withHiatus_end(LocalDate.parse("2020-11-01"))
                                .withAccepts(List.of(Reviewer.Accepts.MULTIMEDIA,
                                        Reviewer.Accepts.PS4, Reviewer.Accepts.PS5)),
                        new Reviewer()
                                .withId(2)
                                .withActive(true)
                                .withFirstName("Ole")
                                .withLastName("Olsen")
                                .withEmail("ole@olsen.dk")
                                .withAddress(
                                        new Address()
                                                .withAddress1("Storegade 99")
                                                .withZip("1111")
                                                .withCity("Storeved")

                                )
                                .withInstitution("Ole Olsens Goodies")
                                .withPaycode(0)
                                .withSubjects(
                                        List.of(
                                                new Subject()
                                                        .withId(5)
                                                        .withName("Multimedie")
                                        )
                                )
                                .withHiatus_begin(LocalDate.parse("2020-11-28"))
                                .withHiatus_end(LocalDate.parse("2020-12-01"))
                                .withAccepts(List.of(Reviewer.Accepts.MULTIMEDIA,
                                        Reviewer.Accepts.PS4, Reviewer.Accepts.PS5))
                )
        );

        final ReviewerList<Reviewer> actual = mapper.readValue(
                get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/reviewers")),
                new TypeReference<>() {});

        assertThat("List of reviewers is just 'Hans Hansen' and 'Ole Olsen'",
                actual, is(expected));
    }

    @Test
    public void listReviewersWithWorkloads() throws JsonProcessingException {
        final ReviewerWithWorkloads reviewer1 = new ReviewerWithWorkloads();
        reviewer1.setId(1);
        reviewer1.setActive(true);
        reviewer1.setFirstName("Hans");
        reviewer1.setLastName("Hansen");
        reviewer1.setEmail("hans@hansen.dk");
        reviewer1.setAddress(
                new Address()
                        .withAddress1("Lillegade 1")
                        .withZip("9999")
                        .withCity("Lilleved"));
        reviewer1.setInstitution("Hans Hansens Bix");
        reviewer1.setPaycode(0);
        reviewer1.setSubjects(
                List.of(
                        new Subject()
                                .withId(3)
                                .withName("Eventyr, fantasy")
                                .withParentId(2),
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer1.setHiatus_begin(LocalDate.parse("2020-10-28"));
        reviewer1.setHiatus_end(LocalDate.parse("2020-11-01"));
        reviewer1.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
        reviewer1.setWeekWorkload(0);
        reviewer1.setWeekBeforeWorkload(0);
        reviewer1.setWeekAfterWorkload(1);

        final ReviewerWithWorkloads reviewer2 = new ReviewerWithWorkloads();
        reviewer2.setId(2);
        reviewer2.setActive(true);
        reviewer2.setFirstName("Ole");
        reviewer2.setLastName("Olsen");
        reviewer2.setEmail("ole@olsen.dk");
        reviewer2.setAddress(
                new Address()
                        .withAddress1("Storegade 99")
                        .withZip("1111")
                        .withCity("Storeved"));
        reviewer2.setInstitution("Ole Olsens Goodies");
        reviewer2.setPaycode(0);
        reviewer2.setSubjects(
                List.of(
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        reviewer2.setHiatus_begin(LocalDate.parse("2020-11-28"));
        reviewer2.setHiatus_end(LocalDate.parse("2020-12-01"));
        reviewer2.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
        reviewer2.setWeekWorkload(0);
        reviewer2.setWeekBeforeWorkload(0);
        reviewer2.setWeekAfterWorkload(0);

        final ReviewerList<ReviewerWithWorkloads> expected = new ReviewerList<ReviewerWithWorkloads>()
                .withReviewers(List.of(reviewer1, reviewer2));

        final Response response = getResponse("v1/api/reviewers",
                Map.of("deadline", "2020-12-01"));

        final ReviewerList<ReviewerWithWorkloads> actual = mapper.readValue(response.readEntity(String.class),
                new TypeReference<>() {});

        assertThat("List of reviewers is just 'Hans Hansen' and 'Ole Olsen'",
                actual, is(expected));
    }
}
