package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.persistence.Reviewer;
import java.util.Map;

public class ReviewerDataChanged {

    Map<String, ChangedValue> diffMap;

    private Reviewer newReviewerData;

    private Reviewer reviewer;

    public Reviewer getNewReviewerData() {
        return newReviewerData;
    }

    public Reviewer getReviewer() {
        return reviewer;
    }

    public ReviewerDataChanged withNewReviewer(Reviewer reviewer) {
        this.newReviewerData = reviewer;
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
                ", newReviewerData=" + newReviewerData +
                ", reviewer=" + reviewer +
                '}';
    }
}
