package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;

import java.io.IOException;

// A plain Java interface (nothing Jakarta-specific about it) with a single method - this is
// the "strategy pattern": callers (TaxonomyCache) depend only on this interface, never on a
// concrete implementation, so which actual data source builds the tree can be swapped without
// touching any calling code. There are two implementations: DM2Builder (fetches from
// rawrepo-record-service over HTTP) and DbTaxonomyBuilder (reads from this project's own
// Postgres tables) - TaxonomyBuilderProducer decides which one to hand out, based on
// configuration, via a CDI @Produces method. See both of those classes for the rest of the
// picture.
public interface TaxonomyBuilder {
    void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException, IOException;
}
