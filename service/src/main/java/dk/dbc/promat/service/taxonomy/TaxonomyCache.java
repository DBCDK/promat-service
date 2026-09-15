package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class TaxonomyCache {
    private final AtomicReference<Taxonomy> current = new AtomicReference<>(new Taxonomy());

    public Taxonomy get() {
        return current.get();
    }

    // Published by ScheduledTaxonomyKafkaSync after every sync run.
    public void set(Taxonomy taxonomy) {
        current.set(taxonomy);
    }
}
