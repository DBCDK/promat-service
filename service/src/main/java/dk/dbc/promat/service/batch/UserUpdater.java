/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.PromatUser;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.util.PromatTaskUtils;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public void updateEditor(Editor editor) {

        try {
            if( !editor.isActive() && editor.getActiveChanged().toInstant().isBefore(ZonedDateTime.now().minusYears(5).toInstant())) {
                LOGGER.info("Editor {} has been inactive for more than 5 years", editor.getId());
                editor.setEmail("");
                editor.setPhone("");
                editor.setDeactivated(Date.from(ZonedDateTime.now().toInstant()));
            }
        } catch (Exception e) {
            LOGGER.error("Unable to update editor with id {}: {}", editor.getId(), e.getMessage());
            metricRegistry.concurrentGauge(userUpdateFailureGaugeMetadata).inc();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateReviewer(Reviewer reviewer) {

        try {
            if( !reviewer.isActive() && reviewer.getActiveChanged().toInstant().isBefore(ZonedDateTime.now().minusYears(5).toInstant())) {
                LOGGER.info("Reviewer {} has been inactive for more than 5 years", reviewer.getId());
                reviewer.setEmail("");
                reviewer.setPhone("");
                reviewer.setPrivateEmail("");
                reviewer.setPrivatePhone("");
                reviewer.setAddress(new Address());
                reviewer.setPrivateAddress(new Address());
                reviewer.setDeactivated(Date.from(ZonedDateTime.now().toInstant()));
            }
        } catch (Exception e) {
            LOGGER.error("Unable to update reviewer with id {}: {}", reviewer.getId(), e.getMessage());
            metricRegistry.concurrentGauge(userUpdateFailureGaugeMetadata).inc();
        }
    }
}
