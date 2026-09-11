package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;

import java.io.IOException;

// Implemented by DM2Builder (fetches from rawrepo-record-service over HTTP) - see
// TaxonomyBuilderProducer for when it's used.
public interface TaxonomyBuilder {
    void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException, IOException;
}
