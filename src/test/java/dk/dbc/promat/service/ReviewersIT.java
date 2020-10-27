package dk.dbc.promat.service;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.entity.Address;
import dk.dbc.promat.service.entity.Reviewer;
import dk.dbc.promat.service.entity.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ReviewersIT extends ContainerTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void crude_test() throws JSONBException {
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
                                                .withZip(9999)
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
                )
        );

        ReviewerList actual = jsonbContext.unmarshall(
                get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/reviewers")),
                ReviewerList.class
        );


        assertThat("List of reviewers is just 'Hans Hansen'",
                actual.toString(),
                is(expected.toString()));

    }

}
