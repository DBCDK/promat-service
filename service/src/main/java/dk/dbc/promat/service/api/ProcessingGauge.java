package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@Startup
@Singleton
public class ProcessingGauge {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingGauge.class);

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    static final Metadata recordHandlerRecordsInProcessingGauge = Metadata.builder()
            .withName("promat_service_record_handler_records_in_processing")
            .withDescription("Number of records with status PROCESSING")
            .withType(MetricType.GAUGE)
            .withUnit("records")
            .build();

    Gauge<Long> recordsInProcessingGauge = () -> {
        TypedQuery<Long> query = entityManager
                .createNamedQuery(PromatCase.GET_COUNT_OF_CASES_IN_PROCESSING_STATE_NAME, Long.class);
        return query.getSingleResult();
    };

    @PostConstruct
    public void register() {
        if (metricRegistry != null) {
            LOGGER.info("Registering {}", recordHandlerRecordsInProcessingGauge.getName());
            metricRegistry.register(recordHandlerRecordsInProcessingGauge, recordsInProcessingGauge);
        } else {
            LOGGER.info("No injected metricRegistry. Unable to register {}", recordHandlerRecordsInProcessingGauge.getName());
        }
    }
}
