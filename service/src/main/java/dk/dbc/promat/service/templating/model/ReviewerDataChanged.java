package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Reviewer;
import java.util.Map;

public class ReviewerDataChanged {

    Map<String, ChangedValue> diffMap;

    private ReviewerRequest reviewerRequest;

    private Reviewer reviewer;

    public ReviewerRequest getReviewerRequest() {
        return reviewerRequest;
    }

    public Reviewer getReviewer() {
        return reviewer;
    }

    public ReviewerDataChanged withReviewerRequest(ReviewerRequest reviewerRequest) {
        this.reviewerRequest = reviewerRequest;
        return this;
    }

    public Map<String, ChangedValue> getDiffMap() {
        return diffMap;
    }

    public ReviewerDataChanged withReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public ReviewerDataChanged withDiff(Map<String, ChangedValue> diffMap) {
        this.diffMap = diffMap;
        return this;
    }

    @Override
    public String toString() {
        return "ReviewerDataChanged{" +
                "diffMap=" + diffMap +
                ", reviewerRequest=" + reviewerRequest +
                ", reviewer=" + reviewer +
                '}';
    }
}
