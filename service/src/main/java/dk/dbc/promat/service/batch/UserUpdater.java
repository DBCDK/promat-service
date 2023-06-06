package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Reviewer;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Date;

@Stateless
public class UserUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserUpdater.class);
    protected static final String METAKOMPASDATA_PRESENT = "true";

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    static final Metadata userUpdateFailureGaugeMetadata = Metadata.builder()
            .withName("promat_service_userupdater_update_failures")
            .withDescription("Number of failing update attempts")
            .withType(MetricType.CONCURRENT_GAUGE)
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
            metricRegistry.concurrentGauge(userUpdateFailureGaugeMetadata).inc();
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
            metricRegistry.concurrentGauge(userUpdateFailureGaugeMetadata).inc();
        }
    }
}
