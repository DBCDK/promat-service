package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;

import java.io.IOException;

// Two implementations: DM2Builder (fetches from rawrepo-record-service over HTTP) and
// DbTaxonomyBuilder (reads from this project's own taxonomy_snapshot table) - see
// TaxonomyBuilderProducer for which one gets used, based on config.
public interface TaxonomyBuilder {
    void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException, IOException;
}
