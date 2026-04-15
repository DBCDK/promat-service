package dk.dbc.promat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TestUtils {
    public static String getResource(String resource) throws IOException {
        return Files.readString(Path.of(getPath(resource)));
    }

    public static String getPath(String resource) {
        return  Objects.requireNonNull(TestUtils.class.getResource(resource)).getPath();
    }
}
