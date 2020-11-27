/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ReviewersIT extends ContainerTest {

    @Test
    public void crude_test() throws JsonProcessingException {
        final ReviewerList expected = new ReviewerList().withReviewers(
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

        final ReviewerList actual = mapper.readValue(
                get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/reviewers")),
                ReviewerList.class);

        assertThat("List of reviewers is just 'Hans Hansen' and 'Ole Olsen'",
                actual.toString(),
                is(expected.toString()));
    }
}
