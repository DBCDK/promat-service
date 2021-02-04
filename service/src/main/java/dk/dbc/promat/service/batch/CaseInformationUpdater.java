/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.persistence.PromatCase;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Stateless
public class CaseInformationUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInformationUpdater.class);

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Inject
    OpenFormatHandler openFormatHandler;

    static final Metadata openformatTimerMetadata = Metadata.builder()
            .withName("promat_service_caseinformationupdater_openformat_timer")
            .withDescription("Openformat response time")
            .withType(MetricType.SIMPLE_TIMER)
            .withUnit(MetricUnits.MILLISECONDS)
            .build();

    static final Metadata caseUpdateFailureGaugeMetadata = Metadata.builder()
            .withName("promat_service_caseinformationupdater_update_failures")
            .withDescription("Number of failing update attempts")
            .withType(MetricType.CONCURRENT_GAUGE)
            .withUnit("failures")
            .build();

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateCaseInformation(PromatCase promatCase) {

        try {
            // Get bibliographic information for the primary faustnumber
            long taskStartTime = System.currentTimeMillis();
            BibliographicInformation bibliographicInformation = openFormatHandler.format(promatCase.getPrimaryFaust());

            if (!bibliographicInformation.isOk()) {
                LOGGER.error("Failed to obtain bibliographic information for case with id {} and primary faust {}",
                        promatCase.getId(), promatCase.getPrimaryFaust());
                metricRegistry.concurrentGauge(caseUpdateFailureGaugeMetadata).inc();
                return;
            }
            metricRegistry.simpleTimer(openformatTimerMetadata).update(Duration.ofMillis(System.currentTimeMillis() - taskStartTime));

            // Update title, if changed
            if (promatCase.getTitle() != bibliographicInformation.getTitle()) {
                LOGGER.info("Updating title: '{}' ==> '{}' of case with id {}", promatCase.getTitle(),
                        bibliographicInformation.getTitle(), promatCase.getId());
                promatCase.setTitle(bibliographicInformation.getTitle());
            }

            // Check if the record has a BKMxxxxxx catalog code, if so - then check if we need to update the case,
            // otherwise check if we need to clear an existing weekcode (record may have been pulled back for further
            // editing by the cataloging team)
            String newCode = getFirstWeekcode(bibliographicInformation.getCatalogcodes());
            if (newCode.isEmpty() && !promatCase.getWeekCode().isEmpty()) {
                LOGGER.info("Weekcode removed from case with id {}. Record of primary faust {} has no BKMxxxxxx catalogcode", promatCase.getId());
                promatCase.setWeekCode("");
            }
            if (!newCode.isEmpty() && promatCase.getWeekCode() != newCode) {
                LOGGER.info("Updating weekcode: '{}' ==> '{}' of case with id {}", promatCase.getWeekCode(), newCode, promatCase.getId());
                promatCase.setWeekCode(newCode);
            }
        } catch(OpenFormatConnectorException e) {
            LOGGER.error("Caught exception when trying to obtain bibliographic information for faust {} in case with id {}: {}",
                    promatCase.getPrimaryFaust(), promatCase.getId(), e.getMessage());
            metricRegistry.concurrentGauge(caseUpdateFailureGaugeMetadata).inc();
        } catch (Exception e) {
            LOGGER.error("Unable to update case with id {}: {}",promatCase.getId(), e.getMessage());
            metricRegistry.concurrentGauge(caseUpdateFailureGaugeMetadata).inc();
        }
    }

    public void resetUpdateCaseFailuresGauge() {
        ConcurrentGauge gauge = metricRegistry.concurrentGauge(caseUpdateFailureGaugeMetadata);
        while (gauge.getCount() > 0) {
            gauge.dec();
        }
    }

    private String getFirstWeekcode(List<String> codes) {

        // Only look after BKM and BKX (express) catalogcodes.
        // If Both exists (as they would for express cases), take the earliest weekcode
        Optional<String> newCode = codes.stream()
                .filter(code -> Arrays.asList("BKM", "BKX").contains(code.substring(0, 3)))
                .sorted(Comparator.comparing(code -> code.substring(3)))
                .findFirst();
        return newCode.isPresent() ? newCode.get() : "";
    }
}
