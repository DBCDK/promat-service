package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatMessage;
import java.util.Objects;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class PromatMessageDto implements Dto {
    private Integer caseId;

    private PromatMessage.Direction direction;
    private String messageText;

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public PromatMessage.Direction getDirection() {
        return direction;
    }

    public void setDirection(PromatMessage.Direction direction) {
        this.direction = direction;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public PromatMessageDto withCaseId(Integer caseId) {
        this.caseId = caseId;
        return this;
    }

    public PromatMessageDto withDirection(PromatMessage.Direction direction) {
        this.direction = direction;
        return this;
    }

    public PromatMessageDto withMessageText(String messageText) {
        this.messageText = messageText;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PromatMessageDto)) {
            return false;
        }
        PromatMessageDto aPromatMessageDto = (PromatMessageDto) o;
        return Objects.equals(caseId, aPromatMessageDto.caseId) &&
                direction == aPromatMessageDto.direction &&
                Objects.equals(messageText, aPromatMessageDto.messageText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, direction, messageText);
    }

    @Override
    public String toString() {
        return "PromatMessageDto{" +
                "caseId=" + caseId +
                ", direction=" + direction +
                ", messageText='" + messageText + '\'' +
                '}';
    }
}