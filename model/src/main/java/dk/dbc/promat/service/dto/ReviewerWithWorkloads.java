package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.Reviewer;

public class ReviewerWithWorkloads extends Reviewer {
    private long weekWorkload;
    private long weekBeforeWorkload;
    private long weekAfterWorkload;

    public long getWeekWorkload() {
        return weekWorkload;
    }

    public void setWeekWorkload(long weekWorkload) {
        this.weekWorkload = weekWorkload;
    }

    public ReviewerWithWorkloads withWeekWorkload(long weekWorkload) {
        this.weekWorkload = weekWorkload;
        return this;
    }

    public long getWeekBeforeWorkload() {
        return weekBeforeWorkload;
    }

    public void setWeekBeforeWorkload(long weekBeforeWorkload) {
        this.weekBeforeWorkload = weekBeforeWorkload;
    }

    public ReviewerWithWorkloads withWeekBeforeWorkload(long weekBeforeWorkload) {
        this.weekBeforeWorkload = weekBeforeWorkload;
        return this;
    }

    public long getWeekAfterWorkload() {
        return weekAfterWorkload;
    }

    public void setWeekAfterWorkload(long weekAfterWorkload) {
        this.weekAfterWorkload = weekAfterWorkload;
    }

    public ReviewerWithWorkloads withWeekAfterWorkload(long weekAfterWorkload) {
        this.weekAfterWorkload = weekAfterWorkload;
        return this;
    }

    @Override
    public String toString() {
        return "ReviewerWithWorkloads{" +
                "id=" + id +
                ", active=" + active +
                ", culrId'=" + culrId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address=" + address +
                ", institution='" + institution + '\'' +
                ", paycode=" + paycode +
                ", hiatusBegin=" + hiatusBegin +
                ", hiatusEnd=" + hiatusEnd +
                ", subjects=" + subjects +
                ", accepts=" + accepts +
                ", note=" + note +
                ", agency=" + agency +
                ", userId=" + userId +
                ", weekWorkload=" + weekWorkload +
                ", weekBeforeWorkload=" + weekBeforeWorkload +
                ", weekAfterWorkload=" + weekAfterWorkload +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ReviewerWithWorkloads that = (ReviewerWithWorkloads) o;

        if (weekWorkload != that.weekWorkload) {
            return false;
        }
        if (weekBeforeWorkload != that.weekBeforeWorkload) {
            return false;
        }
        return weekAfterWorkload == that.weekAfterWorkload;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (weekWorkload ^ (weekWorkload >>> 32));
        result = 31 * result + (int) (weekBeforeWorkload ^ (weekBeforeWorkload >>> 32));
        result = 31 * result + (int) (weekAfterWorkload ^ (weekAfterWorkload >>> 32));
        return result;
    }
}
