package dk.dbc.promat.service.batch;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Payara provides the MicroProfile Health implementation at runtime - this minimal one stands in for it in unit tests
final class TestHealthCheckResponses {
    private TestHealthCheckResponses() {
    }

    static void install() {
        HealthCheckResponse.setResponseProvider(() -> new HealthCheckResponseBuilder() {
            private String name;
            private boolean up;
            private final Map<String, Object> data = new HashMap<>();

            @Override public HealthCheckResponseBuilder name(String name) { this.name = name; return this; }
            @Override public HealthCheckResponseBuilder withData(String key, String value) { data.put(key, value); return this; }
            @Override public HealthCheckResponseBuilder withData(String key, long value) { data.put(key, value); return this; }
            @Override public HealthCheckResponseBuilder withData(String key, boolean value) { data.put(key, value); return this; }
            @Override public HealthCheckResponseBuilder up() { up = true; return this; }
            @Override public HealthCheckResponseBuilder down() { up = false; return this; }
            @Override public HealthCheckResponseBuilder status(boolean up) { this.up = up; return this; }
            @Override public HealthCheckResponse build() {
                return new HealthCheckResponse(name, up ? HealthCheckResponse.Status.UP : HealthCheckResponse.Status.DOWN,
                        data.isEmpty() ? Optional.empty() : Optional.of(data));
            }
        });
    }
}
