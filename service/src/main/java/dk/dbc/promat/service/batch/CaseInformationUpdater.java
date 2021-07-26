/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.FulltextHandler;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.util.PromatTaskUtils;
import dk.dbc.promat.service.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Stateless
public class CaseInformationUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInformationUpdater.class);
    protected static final String METAKOMPASDATA_PRESENT = "true";

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Inject
    OpenFormatHandler openFormatHandler;

    @EJB
    Repository repository;

    @Inject
    ContentLookUp contentLookUp;


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
                LOGGER.error("Failed to obtain bibliographic information for case with id {} and primary faust {}: {}",
                        promatCase.getId(), promatCase.getPrimaryFaust(), bibliographicInformation.getError());
                metricRegistry.concurrentGauge(caseUpdateFailureGaugeMetadata).inc();
                return;
            }
            metricRegistry.simpleTimer(openformatTimerMetadata).update(Duration.ofMillis(System.currentTimeMillis() - taskStartTime));
            // Update title, if changed
            if (useSameOrUpdateValue(promatCase.getTitle(), bibliographicInformation.getTitle(), false)) {
                LOGGER.info("Updating title: '{}' ==> '{}' of case with id {}", promatCase.getTitle(),
                        bibliographicInformation.getTitle(), promatCase.getId());
                promatCase.setTitle(bibliographicInformation.getTitle());
            }

            // Update author, if changed
            if(useSameOrUpdateValue(promatCase.getAuthor(), bibliographicInformation.getCreator(), false)) {
                LOGGER.info("Updating author: '{}' ==> '{}' of case with id {}", promatCase.getAuthor(),
                        bibliographicInformation.getCreator(), promatCase.getId());
                promatCase.setAuthor(bibliographicInformation.getCreator());
            }

            // Update publisher, if changed
            if(useSameOrUpdateValue(promatCase.getPublisher(), bibliographicInformation.getPublisher(), false)) {
                LOGGER.info("Updating publisher: '{}' ==> '{}' of case with id {}", promatCase.getPublisher(),
                        bibliographicInformation.getPublisher(), promatCase.getId());
                promatCase.setPublisher(bibliographicInformation.getPublisher());
            }

            // Check if the record has a BKMxxxxxx catalog code, if so - then check if we need to update the case,
            // otherwise check if we need to clear an existing weekcode (record may have been pulled back for further
            // editing by the cataloging team)
            String newCode = getFirstWeekcode(bibliographicInformation.getCatalogcodes());
            if( useSameOrUpdateValue(promatCase.getWeekCode(), newCode, true)) {
                LOGGER.info("Updating weekcode: '{}' ==> '{}' of case with id {}", promatCase.getWeekCode(), newCode, promatCase.getId());
                promatCase.setWeekCode(newCode);
            }
            // Check if the case has status 'APPROVED' and we have reached the week specified by the weekcode
            if( CaseStatus.APPROVED == promatCase.getStatus() && weekcodeMatchOrBefore(promatCase) ) {
                LOGGER.info("Changing status on case {} to PENDING_EXPORT since weekcode {} is actual or previous week", promatCase.getId(), promatCase.getWeekCode());
                repository.assignFaustnumber(promatCase);
                promatCase.setStatus(CaseStatus.PENDING_EXPORT);
            }

            // Check and update case with Metakompasdata
            checkAndUpdateCaseWithMetakompasdata(promatCase, bibliographicInformation);
            //
            // Status is 'PENDING_EXTERNAL'. Now do last check of metakompas data before setting
            // final state: APPROVED.
            //
            if (CaseStatus.PENDING_EXTERNAL == promatCase.getStatus()) {
                LOGGER.info("Case '{}' is in PENDING_EXTERNAL state. Checking that all 'metakompas' " +
                        "data has been registered", promatCase.getId());
                boolean approved = promatCase.getTasks().stream().allMatch(promatTask -> promatTask.getApproved() != null);
                promatCase.setStatus(approved ? CaseStatus.APPROVED : CaseStatus.PENDING_EXTERNAL);
            }
            //
            // A given ebook or book might be present in the "material content repo" (DMAT) for handout to reviewer,
            // through promat-frontend.
            // If a fulltextlink is already present on this case: Assume that everything is ok.
            if (promatCase.getMaterialType() == MaterialType.BOOK &&
                    (promatCase.getFulltextLink() == null ||
                    promatCase.getFulltextLink().isBlank())) {
                Optional<String > fullTextLink = contentLookUp.lookUpContent(promatCase.getPrimaryFaust());
                if (fullTextLink.isPresent()) {
                    promatCase.setFulltextLink(fullTextLink.get());
                    LOGGER.info("Fulltextlink '{}' has been added to case:{}", fullTextLink, promatCase.getId());
                }
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

    private void checkAndUpdateCaseWithMetakompasdata(PromatCase promatCase, BibliographicInformation bibliographicInformation) {

        // All metakompas tasks
        for (PromatTask task : PromatTaskUtils.getTasksOfType(promatCase, TaskFieldType.METAKOMPAS)) {

            // In theory targetFaust can be empty, signifying that the primary faust from the case is to be used.
            List<String> fausts = task.getTargetFausts() != null ? task.getTargetFausts() : List.of(promatCase.getPrimaryFaust());

            boolean allIsPresent = fausts
                    .stream().allMatch(faust -> {
                        try {
                            String metakompassubject = openFormatHandler.format(faust).getMetakompassubject();
                            String present = metakompassubject != null ? metakompassubject.strip() : null;
                            return METAKOMPASDATA_PRESENT.equals(present);
                        } catch (OpenFormatConnectorException e) {
                            LOGGER.error("Unable to look up faust {}, {}", faust, e);
                        }
                        return false;
                    });
            if (allIsPresent) {
                LOGGER.info("Updating metakompas for fausts: '{}' ==> '{}' of case with id {}. Taskid is '{}'",
                        promatCase.getRelatedFausts(), METAKOMPASDATA_PRESENT, promatCase.getId(), task.getId());
                task.setData(METAKOMPASDATA_PRESENT);
                task.setApproved(LocalDate.now());
            }
        }
    }

    public boolean useSameOrUpdateValue(String currentValue, String newValue, boolean allowNullAsNewValue) {
        if (newValue == null) {
            return allowNullAsNewValue;
        }
        if (currentValue == null) {
            return true;
        }
        if (!currentValue.equals(newValue)) {
            return true;
        }
        return false;
    }

    public void resetUpdateCaseFailuresGauge() {
        ConcurrentGauge gauge = metricRegistry.concurrentGauge(caseUpdateFailureGaugeMetadata);
        while (gauge.getCount() > 0) {
            gauge.dec();
        }
    }

    private String getFirstWeekcode(List<String> codes) {

        // Only look after BKM and BKX (express) catalogcodes.
        // If Both exists (as they would for express cases), BKX takes precedence
        Optional<String> newCode = codes.stream()
                .filter(code -> Arrays.asList("BKM", "BKX").contains(code.substring(0, 3)))
                .sorted(Comparator.comparing(code -> code.substring(0, 3)))
                .sorted(Comparator.reverseOrder())
                .findFirst();
        return newCode.isPresent() ? newCode.get() : "";
    }

    private Boolean weekcodeMatchOrBefore(PromatCase promatCase) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyw", new Locale("da", "DK"));

        LocalDate date = LocalDate.now();
        LOGGER.info("Today is {} weekcode {}", date, date.format(formatter));

        // We want to promote cases with next weeks weekcode (effectively current weekcode by friday = shiftday)
        date = date.plusWeeks(1);
        LOGGER.info("Next week is {} weekcode {}", date, date.format(formatter));

        // Get todays weekcode
        String todaysWeekcode = date.format(formatter);
        Integer today = Integer.parseInt(todaysWeekcode);

        // Do not use trimmedWeekcode, it is set by the db on update, and the entity
        // might not have been commited before we get to this line
        if( promatCase.getWeekCode() != null && !promatCase.getWeekCode().isEmpty() ) {
            LOGGER.info("Case has weekcode {}", promatCase.getWeekCode());
            Integer caseWeekcode = Integer.parseInt(promatCase.getWeekCode().substring(3));
            LOGGER.info("caseWeekcode = {}, today (shifted) = {}", caseWeekcode, today);
            return caseWeekcode <= today;
        }

        LOGGER.info("Case has no weekcode yet, so no weekcode match");
        return false;
    }
}
