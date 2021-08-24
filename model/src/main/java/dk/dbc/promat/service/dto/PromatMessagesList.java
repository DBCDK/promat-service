package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatMessage;
import java.util.List;
import java.util.Objects;

public class PromatMessagesList {
    private int numFound = 0;
    List<PromatMessage> promatMessages = null;

    public PromatMessagesList withPromatMessages(List<PromatMessage> promatMessages) {
        if (promatMessages != null) {
            numFound = promatMessages.size();
        } else {
            numFound = 0;
        }
        this.promatMessages = promatMessages;
        return this;
    }

    public int getNumFound() { return numFound; }

    public List<PromatMessage> getPromatMessages() { return promatMessages; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PromatMessagesList)) {
            return false;
        }
        PromatMessagesList that = (PromatMessagesList) o;
        return numFound == that.numFound &&
                Objects.equals(promatMessages, that.promatMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numFound, promatMessages);
    }

    @Override
    public String toString() {
        return "PromatMessagesList{" +
                "numFound=" + numFound +
                ", promatMessages=" + promatMessages +
                '}';
    }
}
