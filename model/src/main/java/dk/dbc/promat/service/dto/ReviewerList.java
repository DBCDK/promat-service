package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.Reviewer;
import java.util.List;

public class ReviewerList<T extends Reviewer> {
    private int numFound = 0;
    List<T> reviewers = null;

    public ReviewerList<T> withReviewers(List<T> reviewers) {
        if (reviewers != null) {
            numFound = reviewers.size();
        } else {
            numFound = 0;
        }
        this.reviewers = reviewers;
        return this;
    }

    public int getNumFound() {
        return numFound;
    }

    public List<T> getReviewers() {
        return reviewers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReviewerList that = (ReviewerList) o;

        if (numFound != that.numFound) return false;
        return reviewers != null ? reviewers.equals(that.reviewers) : that.reviewers == null;
    }

    @Override
    public int hashCode() {
        int result = numFound;
        result = 31 * result + (reviewers != null ? reviewers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReviewerList{" +
                "numFound=" + numFound +
                ", reviewers=" + reviewers +
                '}';
    }
}
