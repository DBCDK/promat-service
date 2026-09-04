package dk.dbc.promat.service.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Small "escape hatches" so a developer can run the real production WAR
// locally (against a local Postgres, or even the staging DB) without it
// doing things that only make sense in a real deployment: running Flyway
// migrations, firing scheduled batch jobs, or sending real emails.
// Everything here is opt-in via env vars (see scripts/common) - in a real
// deployment none of these are set, so behavior is unchanged.
public final class RuntimeGuards {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeGuards.class);

    // Kubernetes injects this into every pod automatically - it's never set
    // on a developer's machine or a plain `docker run` unless someone fakes
    // it. Real deployments (staging and prod) always run inside a cluster,
    // so this is a reliable "is this actually a real deployment" check that
    // doesn't depend on anyone remembering to unset these guards outside
    // of local dev.
    private static final String KUBERNETES_MARKER_ENV = "KUBERNETES_SERVICE_HOST";
    public static final String SKIP_MIGRATIONS_ENV = "PROMAT_SKIP_MIGRATIONS";
    public static final String DISABLE_SCHEDULED_JOBS_ENV = "PROMAT_DISABLE_SCHEDULED_JOBS";
    public static final String DISABLE_OUTBOUND_MAIL_ENV = "PROMAT_DISABLE_OUTBOUND_MAIL";

    private RuntimeGuards() {}

    public static boolean disableScheduledJobs() {
        return isEnabled(DISABLE_SCHEDULED_JOBS_ENV);
    }

    public static boolean disableOutboundMail() {
        return isEnabled(DISABLE_OUTBOUND_MAIL_ENV);
    }

    public static boolean skipMigrations() {
        return isEnabled(SKIP_MIGRATIONS_ENV);
    }

    // We support both System.getenv and System.getProperty here because a
    // guard needs to work the same way whether you're running the packaged
    // Docker image (set via `docker run -e ...`) or launching the WAR
    // straight from an IDE/Maven (set via `-DPROMAT_SKIP_MIGRATIONS=true`).
    private static boolean isEnabled(String name) {
        boolean requested = Boolean.parseBoolean(System.getenv(name))
                || Boolean.parseBoolean(System.getProperty(name));
        if (!requested) {
            return false;
        }
        if (System.getenv(KUBERNETES_MARKER_ENV) != null) {
            LOGGER.warn("Ignoring {}=true because this process is running inside a Kubernetes cluster " +
                    "({} is set) - this guard is a local-dev-only escape hatch and must not affect a real deployment",
                    name, KUBERNETES_MARKER_ENV);
            return false;
        }
        return true;
    }
}
