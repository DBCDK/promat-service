package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.connectors.OpenFormatConnectorProducer;

public class NotificationFactoryIT {
    public static NotificationFactory getNotificationFactory(String wiremockHost) {
        NotificationFactory ntf = new NotificationFactory();
        ntf.reviewerDiffer = new ReviewerDiffer();
        ntf.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorProducer.produce(wiremockHost));
        return ntf;
    }
}
