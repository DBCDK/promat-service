package dk.dbc.promat.service.taxonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TestBase {
    public String getResource(String resource) throws IOException {
        return Files.readString(Path.of(getPath(resource)));
    }

    public String getPath(String resource) {
        return  Objects.requireNonNull(TestBase.class.getResource(resource)).getPath();
    }
}
