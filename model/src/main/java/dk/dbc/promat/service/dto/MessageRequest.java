package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatMessage;
import java.util.Objects;

public class MessageRequest implements Dto {

    private PromatMessage.Direction direction;
    private String messageText;



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

    public MessageRequest withDirection(PromatMessage.Direction direction) {
        this.direction = direction;
        return this;
    }

    public MessageRequest withMessageText(String messageText) {
        this.messageText = messageText;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageRequest)) {
            return false;
        }
        MessageRequest aMessageRequest = (MessageRequest) o;
        return direction == aMessageRequest.direction &&
                Objects.equals(messageText, aMessageRequest.messageText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, messageText);
    }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "direction=" + direction +
                ", messageText='" + messageText + '\'' +
                '}';
    }
}