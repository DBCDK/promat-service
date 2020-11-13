package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationType;

public class NotificationFactory {
    private final Renderer renderer;
    private static final NotificationFactory notificationFactory;

    static {
        notificationFactory = new NotificationFactory(new Renderer());
    }

    public NotificationFactory(Renderer renderer) {
        this.renderer = renderer;
    }

    public static NotificationFactory getInstance() {
        return notificationFactory;
    }

    public Notification of(NotificationType notificationType, Object model) {
        Notification notification = new Notification();
        switch(notificationType) {
            case CASE_ASSIGNED:
                return notification
                        .withToAddress(((Case) model).getReviewer().getEmail())
                        .withSubject("Ny promat anmeldelse")
                        .withBodyText(renderer.render("reviewer_assign_to_case.jte", model));
            default: return null;
        }
    }
}
