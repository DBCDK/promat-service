package dk.dbc.promat.service.templating.model;

import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatMessage;

public class MailToReviewerOnNewMessage {
    PromatMessage message;
    PromatCase promatCase;

    public PromatMessage getMessage() {
        return message;
    }

    public void setMessage(PromatMessage message) {
        this.message = message;
    }

    public PromatCase getPromatCase() {
        return promatCase;
    }

    public void setPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
    }

    public MailToReviewerOnNewMessage withPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
        return this;
    }

    public MailToReviewerOnNewMessage withMessage(PromatMessage message) {
        this.message = message;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailToReviewerOnNewMessage)) {
            return false;
        }

        MailToReviewerOnNewMessage mailObject = (MailToReviewerOnNewMessage) o;

        if (!message.equals(mailObject.message)) {
            return false;
        }
        return promatCase.equals(mailObject.promatCase);
    }

    @Override
    public int hashCode() {
        int result = message.hashCode();
        result = 31 * result + promatCase.hashCode();
        return result;
    }
}
