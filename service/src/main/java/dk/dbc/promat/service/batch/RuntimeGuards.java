package dk.dbc.promat.service.batch;

// Small "escape hatches" so a developer can run the real production WAR
// locally (against a local Postgres, or even the staging DB) without it
// doing things that only make sense in a real deployment: running Flyway
// migrations, firing scheduled batch jobs, or sending real emails.
// Everything here is opt-in via env vars (see scripts/common) - in a real
// deployment none of these are set, so behavior is unchanged.
public final class RuntimeGuards {
    // These are just the env var *names*. Nothing reads the values until
    // one of the isXxx() methods below is called - see @PostConstruct in
    // DatabaseMigrator for an example caller.
    public static final String SKIP_MIGRATIONS_ENV = "PROMAT_SKIP_MIGRATIONS";
    public static final String DISABLE_SCHEDULED_JOBS_ENV = "PROMAT_DISABLE_SCHEDULED_JOBS";
    public static final String DISABLE_OUTBOUND_MAIL_ENV = "PROMAT_DISABLE_OUTBOUND_MAIL";

    // Private constructor + all-static methods: this class is never
    // instantiated, it's just a namespace for these helper functions
    // (a common Java pattern for stateless utility classes).
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

    // Checks both a real environment variable (System.getenv, e.g. set via
    // `docker run -e ...`) and a JVM system property (System.getProperty,
    // e.g. set via `-DPROMAT_SKIP_MIGRATIONS=true`), so it works whether
    // you're configuring the container or just the JVM directly.
    private static boolean isEnabled(String name) {
        return Boolean.parseBoolean(System.getenv(name))
                || Boolean.parseBoolean(System.getProperty(name));
    }
}
