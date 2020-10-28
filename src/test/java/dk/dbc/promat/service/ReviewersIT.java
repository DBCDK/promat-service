package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ReviewersIT extends ContainerTest {

    @Test
    public void crude_test() throws JsonProcessingException {
        ReviewerList expected = new ReviewerList().withReviewers(
                List.of(
                        new Reviewer()
                                .withId(1)
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
                )
        );

        ReviewerList actual = mapper.readValue(
                get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/reviewers")),
                ReviewerList.class
        );

        assertThat("List of reviewers is just 'Hans Hansen'",
                actual.toString(),
                is(expected.toString()));
    }
}
