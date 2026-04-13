package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class TaxonomyCache {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TaxonomyCache.class);
    private TaxonomyBuilder builder;

    @Inject
    public TaxonomyCache(TaxonomyBuilder builder) {
        this.builder = builder;
    }
    protected TaxonomyCache() {}
    private final AtomicReference<Taxonomy> current = new AtomicReference<>(new Taxonomy());

    public Taxonomy get() {
        return current.get();
    }

    public void refresh() throws TaxonomyException, IOException {
        if (builder == null) {
            LOGGER.error("No builder (DM2 or DM3) registered.");
            return;
        }
        Taxonomy next = new Taxonomy();
        builder.buildTaxonomy(next);
        current.set(next);
    }
}
