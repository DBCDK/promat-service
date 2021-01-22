package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatMessage;
import java.util.Objects;

public class MessageRequestDto implements Dto {

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

    public MessageRequestDto withDirection(PromatMessage.Direction direction) {
        this.direction = direction;
        return this;
    }

    public MessageRequestDto withMessageText(String messageText) {
        this.messageText = messageText;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageRequestDto)) {
            return false;
        }
        MessageRequestDto aMessageRequestDto = (MessageRequestDto) o;
        return direction == aMessageRequestDto.direction &&
                Objects.equals(messageText, aMessageRequestDto.messageText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, messageText);
    }

    @Override
    public String toString() {
        return "MessageRequestDto{" +
                "direction=" + direction +
                ", messageText='" + messageText + '\'' +
                '}';
    }
}