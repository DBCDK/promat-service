/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

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

@Stateless
public class CaseInformationUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInformationUpdater.class);

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

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
        try {return;
            /*long taskStartTime = System.currentTimeMillis();

            // Todo: Call openformat
            // %TITLE% = [openformat.title]
            // %WEEKCODE% = [openformat.weekcode]
            metricRegistry.simpleTimer(openformatTimerMetadata).update(Duration.ofMillis(System.currentTimeMillis() - taskStartTime));

            // Todo: Update case information
            /*if (promatCase.getTitle() != %TITLE%) {
                LOGGER.info("Updating title: '{}' == '{}' of case {}", promatCase.getTitle(), %TITLE%, promatCase.getId());
                promatCase.setTitle(%TITLE%);
            }
            if (promatCase.getWeekCode() != %WEEKCODE%) {
                LOGGER.info("Updating weekcode: '{}' == '{}' of case {}", promatCase.getWeekcode(), %WEEKCODE%, promatCase.getId());
                promatCase.setWeekCode(%WEEKCODE%);
            }*/
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
}
