package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;

import java.io.IOException;

public interface TaxonomyBuilder {
    void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException, IOException;
}
