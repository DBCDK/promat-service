/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.NotificationType;
import dk.dbc.promat.service.persistence.PromatCase;

public class NotificationFactory {
    public class ValidateException extends Exception {
        ValidateException(String reason) {
            super(reason);
        }
    }
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

    public Notification of(NotificationType notificationType, Object model) throws ValidateException {
        Notification notification = new Notification();
        switch(notificationType) {
            case CASE_ASSIGNED:
                PromatCase promatCase = (PromatCase) model;
                if (promatCase.getEditor() == null || promatCase.getDeadline() == null) {
                    throw new ValidateException("Editor or deadline is null");
                }

                return notification
                        .withToAddress(((PromatCase) model).getReviewer().getEmail())
                        .withSubject("Ny promat anmeldelse")
                        .withBodyText(renderer.render("reviewer_assign_to_case.jte", model))
                        .withStatus(NotificationStatus.PENDING);
            default: return null;
        }
    }
}
