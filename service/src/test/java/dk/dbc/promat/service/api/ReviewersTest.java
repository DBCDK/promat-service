package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class ReviewersTest {

    @Test
    void workloadDateIntervals() {
        final LocalDate date = LocalDate.of(2020, 12, 1);
        final Reviewers.WorkloadDateIntervals workloadDateIntervals = Reviewers.WorkloadDateIntervals.from(date);
        assertThat("week begin", workloadDateIntervals.getWeekBegin(),
                is(LocalDate.of(2020, 11, 30)));
        assertThat("week end", workloadDateIntervals.getWeekEnd(),
                is(LocalDate.of(2020, 12, 6)));
        assertThat("week before begin", workloadDateIntervals.getWeekBeforeBegin(),
                is(LocalDate.of(2020, 11, 23)));
        assertThat("week before end", workloadDateIntervals.getWeekBeforeEnd(),
                is(LocalDate.of(2020, 11, 29)));
        assertThat("week after begin", workloadDateIntervals.getWeekAfterBegin(),
                is(LocalDate.of(2020, 12, 7)));
        assertThat("week after end", workloadDateIntervals.getWeekAfterEnd(),
                is(LocalDate.of(2020, 12, 13)));
    }

    @Test
    void reviewerEquals() {
        final Reviewer expectedReviewer = new Reviewer();
        expectedReviewer.setId(1);
        expectedReviewer.setActive(true);
        expectedReviewer.setFirstName("Hans");
        expectedReviewer.setLastName("Hansen");
        expectedReviewer.setPaycode(123);
        expectedReviewer.setInstitution("Hans Hansens Bix");
        expectedReviewer.setSubjects(
                List.of(
                        new Subject()
                                .withId(3)
                                .withName("Eventyr, fantasy")
                                .withParentId(2),
                        new Subject()
                                .withId(5)
                                .withName("Multimedie")));
        expectedReviewer.setHiatusBegin(LocalDate.parse("2020-10-28"));
        expectedReviewer.setHiatusEnd(LocalDate.parse("2020-11-01"));
        expectedReviewer.setAccepts(List.of(
                Reviewer.Accepts.MULTIMEDIA, Reviewer.Accepts.PS4, Reviewer.Accepts.PS5));
        expectedReviewer.setNote("note1");
        expectedReviewer.setCapacity(1);
        expectedReviewer.setSubjectNotes(List.of());
        expectedReviewer.setEmail("hans@hansen.dk");
        expectedReviewer.setAddress(
                new Address()
                        .withAddress1("Lillegade 1")
                        .withZip("9999")
                        .withCity("Lilleved")
                        .withSelected(true));
        expectedReviewer.setPrivateAddress(
                new Address()
                        .withAddress1("Storegade 1")
                        .withZip("1111")
                        .withCity("Storeved")
                        .withSelected(false));
        expectedReviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        expectedReviewer.setAgency("300100");
        expectedReviewer.setUserId("knud1");

        final Reviewer actual = new Reviewer();
        actual.setId(expectedReviewer.getId());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setActive(expectedReviewer.isActive());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setFirstName(expectedReviewer.getFirstName());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setLastName(expectedReviewer.getLastName());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setPaycode(expectedReviewer.getPaycode());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setInstitution(expectedReviewer.getInstitution());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setSubjects(expectedReviewer.getSubjects());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setHiatusBegin(expectedReviewer.getHiatusBegin());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setHiatusEnd(expectedReviewer.getHiatusEnd());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setAccepts(expectedReviewer.getAccepts());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setNote(expectedReviewer.getNote());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setCapacity(expectedReviewer.getCapacity());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setSubjectNotes(expectedReviewer.getSubjectNotes());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setEmail(expectedReviewer.getEmail());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setAddress(expectedReviewer.getAddress());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setPrivateAddress(expectedReviewer.getPrivateAddress());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setActiveChanged(expectedReviewer.getActiveChanged());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setAgency(expectedReviewer.getAgency());
        assertThat(actual, is(not(expectedReviewer)));
        assertThat(expectedReviewer, is(not(actual)));

        actual.setUserId(expectedReviewer.getUserId());
        assertThat(actual, is(expectedReviewer));
        assertThat(expectedReviewer, is(actual));
    }

    @Test
    void addressEquals() {
        final Address expected = new Address()
                .withAddress1("Lillegade 1")
                .withAddress2("Lillestrup")
                .withZip("9999")
                .withCity("Lilleved")
                .withSelected(true);

        final Address actual = new Address();
        actual.setAddress1(expected.getAddress1());
        assertThat(actual, is(not(expected)));
        assertThat(expected, is(not(actual)));

        actual.setAddress2(expected.getAddress2());
        assertThat(actual, is(not(expected)));
        assertThat(expected, is(not(actual)));

        actual.setZip(expected.getZip());
        assertThat(actual, is(not(expected)));
        assertThat(expected, is(not(actual)));

        actual.setCity(expected.getCity());
        assertThat(actual, is(not(expected)));
        assertThat(expected, is(not(actual)));

        actual.setSelected(expected.getSelected());
        assertThat(actual, is(expected));
        assertThat(expected, is(actual));
    }

    @Test
    void reviewerWithWorkloadsEquals() {
        final Reviewer reviewer = new Reviewer();
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
        reviewer.setEmail("hans@hansen.dk");
        reviewer.setAddress(
                new Address()
                        .withAddress1("Lillegade 1")
                        .withZip("9999")
                        .withCity("Lilleved")
                        .withSelected(true));
        reviewer.setPrivateAddress(
                new Address()
                        .withAddress1("Storegade 1")
                        .withZip("1111")
                        .withCity("Storeved")
                        .withSelected(false));
        reviewer.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        reviewer.setAgency("300100");
        reviewer.setUserId("knud1");

        final ReviewerWithWorkloads expectedReviewerWithWorkloads = reviewer.toReviewerWithWorkloads();

        final ReviewerWithWorkloads actual = new ReviewerWithWorkloads();
        actual.setId(expectedReviewerWithWorkloads.getId());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setActive(expectedReviewerWithWorkloads.isActive());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setFirstName(expectedReviewerWithWorkloads.getFirstName());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setLastName(expectedReviewerWithWorkloads.getLastName());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setPaycode(expectedReviewerWithWorkloads.getPaycode());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setInstitution(expectedReviewerWithWorkloads.getInstitution());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setSubjects(expectedReviewerWithWorkloads.getSubjects());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setHiatusBegin(expectedReviewerWithWorkloads.getHiatusBegin());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setHiatusEnd(expectedReviewerWithWorkloads.getHiatusEnd());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setAccepts(expectedReviewerWithWorkloads.getAccepts());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setNote(expectedReviewerWithWorkloads.getNote());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setCapacity(expectedReviewerWithWorkloads.getCapacity());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setSubjectNotes(expectedReviewerWithWorkloads.getSubjectNotes());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setEmail(expectedReviewerWithWorkloads.getEmail());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setAddress(expectedReviewerWithWorkloads.getAddress());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setPrivateAddress(expectedReviewerWithWorkloads.getPrivateAddress());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setActiveChanged(expectedReviewerWithWorkloads.getActiveChanged());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setAgency(expectedReviewerWithWorkloads.getAgency());
        assertThat(actual, is(not(expectedReviewerWithWorkloads)));
        assertThat(expectedReviewerWithWorkloads, is(not(actual)));

        actual.setUserId(expectedReviewerWithWorkloads.getUserId());
        assertThat(actual, is(expectedReviewerWithWorkloads));
        assertThat(expectedReviewerWithWorkloads, is(actual));
    }
}
