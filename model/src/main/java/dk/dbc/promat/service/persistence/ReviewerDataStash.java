package dk.dbc.promat.service.persistence;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;

@NamedQueries({
        @NamedQuery(
                name = ReviewerDataStash.GET_STASH_FROM_REVIEWER,
                query = ReviewerDataStash.GET_STASH_FROM_REVIEWER_QUERY)
})

@Entity
public class ReviewerDataStash {
    private static Logger LOGGER = LoggerFactory.getLogger(ReviewerDataStash.class);
    private static ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();
    public final static String GET_STASH_FROM_REVIEWER = "ReviewerDataStash.getFromReviewer";
    public final static String GET_STASH_FROM_REVIEWER_QUERY =
            "SELECT r FROM ReviewerDataStash r where r.reviewerId = :reviewerId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;
    protected Integer reviewerId;
    protected String reviewer;

    protected LocalDateTime stashTime;

    public ReviewerDataStash withReviewer(String reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getStashTime() {
        return stashTime;
    }

    public ReviewerDataStash withStashTime(LocalDateTime stashTime) {
        this.stashTime = stashTime;
        return this;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public ReviewerDataStash withReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReviewerDataStash)) return false;
        ReviewerDataStash that = (ReviewerDataStash) o;
        return Objects.equals(id, that.id) && Objects.equals(reviewerId, that.reviewerId) && Objects.equals(reviewer, that.reviewer) && Objects.equals(stashTime, that.stashTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reviewerId, reviewer, stashTime);
    }

    @Override
    public String toString() {
        return "ReviewerDataStash{" +
                "id=" + id +
                ", reviewerId=" + reviewerId +
                ", reviewer='" + reviewer + '\'' +
                ", stashTime=" + stashTime +
                '}';
    }

    public static ReviewerDataStash fromReviewer(Reviewer reviewer) throws JsonProcessingException {
        ReviewerDataStash reviewerDataStash = new ReviewerDataStash()
                .withStashTime(LocalDateTime.now())
                .withReviewer(mapper.writeValueAsString(reviewer))
                .withReviewerId(reviewer.getId());
        LOGGER.info("ReviewerDataStash from reviewer {}", reviewerDataStash);
        return reviewerDataStash;
    }

    public Reviewer toReviewer() throws JsonProcessingException {
        return mapper.readValue(reviewer, Reviewer.class);
    }
}
