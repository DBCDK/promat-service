package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Taxonomy;

import java.io.FileInputStream;
import java.io.IOException;

public class FromFileBuilder extends DM2Builder {
    String filename;


    public FromFileBuilder(String filename) {
        super();
        this.filename = filename;
    }

    @Override
    public void buildTaxonomy(Taxonomy taxonomy) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(filename)) {
            build(inputStream, taxonomy);
        }
    }
}
