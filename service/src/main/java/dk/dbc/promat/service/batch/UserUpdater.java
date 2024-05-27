package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Reviewer;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Date;

@Stateless
public class UserUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserUpdater.class);

    @Inject
    MetricRegistry metricRegistry;

    static final Metadata userUpdateFailureCounterMetadata = Metadata.builder()
            .withName("promat_service_userupdater_update_failures")
            .withDescription("Number of failing update attempts")
            .withUnit("failures")
            .build();

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deactivateEditor(Editor editor) {

        try {
            editor.setEmail("");
            editor.setPhone("");
            editor.setDeactivated(Date.from(ZonedDateTime.now().toInstant()));
        } catch (Exception e) {
            LOGGER.error("Unable to update editor with id {}: {}", editor.getId(), e.getMessage());
            metricRegistry.counter(userUpdateFailureCounterMetadata).inc();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deactivateReviewer(Reviewer reviewer) {

        try {
            reviewer.setEmail("");
            reviewer.setPhone("");
            reviewer.setPrivateEmail("");
            reviewer.setPrivatePhone("");
            reviewer.setAddress(new Address());
            reviewer.setPrivateAddress(new Address());
            reviewer.setDeactivated(Date.from(ZonedDateTime.now().toInstant()));
        } catch (Exception e) {
            LOGGER.error("Unable to update reviewer with id {}: {}", reviewer.getId(), e.getMessage());
            metricRegistry.counter(userUpdateFailureCounterMetadata).inc();
        }
    }
}
