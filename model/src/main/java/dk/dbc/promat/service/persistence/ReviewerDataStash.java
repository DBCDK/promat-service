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
    private static ObjectMapper mapper = new ObjectMapper();
    public final static String GET_STASH_FROM_REVIEWER = "ReviewerDataStash.getFromReviewer";
    public final static String GET_STASH_FROM_REVIEWER_QUERY =
            "SELECT r FROM ReviewerDataStash r where r.reviewerId = :reviewerId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer reviewerId;

    @Column(name = "reviewer")
    private String reviewerData;
    private LocalDateTime stashTime;

    public ReviewerDataStash withReviewerData(String reviewerData) {
        this.reviewerData = reviewerData;
        return this;
    }

    public ReviewerDataStash withId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getStashTime() {
        return stashTime;
    }

    public void setStashTime(LocalDateTime stashTime) {
        this.stashTime = stashTime;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReviewerDataStash)) return false;
        ReviewerDataStash that = (ReviewerDataStash) o;
        return Objects.equals(id, that.id) && Objects.equals(reviewerId, that.reviewerId) && Objects.equals(reviewerData, that.reviewerData) && Objects.equals(stashTime, that.stashTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reviewerId, reviewerData, stashTime);
    }

    @Override
    public String toString() {
        return "ReviewerDataStash{" +
                "id=" + id +
                ", reviewerId=" + reviewerId +
                ", reviewerData='" + reviewerData + '\'' +
                ", stashTime=" + stashTime +
                '}';
    }

    public static ReviewerDataStash fromReviewer(Reviewer reviewer) throws JsonProcessingException {
        return new ReviewerDataStash()
                .withId(reviewer.getId())
                .withReviewerData(mapper.writeValueAsString(reviewer));
    }

    public Reviewer toReviewer() {
        return mapper.convertValue(reviewerData, Reviewer.class);
    }
}
