package dk.dbc.promat.service.templating;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.connectors.FbiApiConnectorProducer;
import dk.dbc.promat.service.connectors.OpenFormatConnectorProducer;

public class NotificationFactoryIT {
    public static NotificationFactory getNotificationFactory(String wiremockHost) {
        NotificationFactory ntf = new NotificationFactory();
        ntf.reviewerDiffer = new ReviewerDiffer();
        ntf.fbiApiHandler = new FbiApiHandler()
                .withConnector(FbiApiConnectorProducer.produce(wiremockHost, wiremockHost, "123456789",
                        "abcdef", new UserAgent("PROMAT_IT")));
        return ntf;
    }
}
