package dk.dbc.promat.service.templating;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.OpenFormatConnectorFactory;
import dk.dbc.promat.service.api.OpenFormatHandler;

public class NotificationFactoryIT {
    public static NotificationFactory getNotificationFactory(String wiremockHost) throws OpenFormatConnectorException {
        NotificationFactory ntf = new NotificationFactory();
        ntf.reviewerDiffer = new ReviewerDiffer();
        ntf.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        return ntf;
    }
}
