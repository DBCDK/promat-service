package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.persistence.Reviewer;

public class HiatusReset {
    private Reviewer reviewer;

    public Reviewer getReviewer() {
        return reviewer;
    }

    public HiatusReset withReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    @Override
    public String toString() {
        return "HiatusReset{" +
                "reviewer=" + reviewer +
                '}';
    }
}
