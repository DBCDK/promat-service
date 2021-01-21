package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.PromatMessage;

public class MarkAsReadRequest implements Dto {
    PromatMessage.Direction direction;

    public PromatMessage.Direction getDirection() {
        return direction;
    }

    public void setDirection(PromatMessage.Direction direction) {
        this.direction = direction;
    }

    public MarkAsReadRequest withDirection(PromatMessage.Direction direction) {
        this.direction = direction;
        return this;
    }

    @Override
    public String toString() {
        return "MarkAsReadRequest{" +
                "direction=" + direction +
                '}';
    }
}
