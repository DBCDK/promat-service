Promat service
==============

### Configuration

For local secrets and overrides, create `.env.local` in the repository root.
The startup scripts load it automatically. Start by copying the committed
template:

```bash
cp .env.local.TEMPLATE .env.local
```

Values not set in `.env.local` fall back to defaults from `scripts/common`.

**Environment variables**

All variables default to sensible values for local development in `scripts/common` — override any of them by exporting a value before running the scripts below.

* PROMAT_DB_URL - database URL (`USER:PASSWORD@HOST:PORT/DBNAME`) of the underlying promat database. Defaults to a local Postgres started via `scripts/start-database`.
* OPENSEARCH_SERVICE_URL - OpenSearch service url
* OPENSEARCH_PROFILE - OpenSearch profile (default `dbckat`)
* OPENSEARCH_AGENCY - OpenSearch agency (default `010100`)
* OPENSEARCH_REPOSITORY - OpenSearch repository (default `rawrepo_basis`)
* RECORD_SERVICE - rawrepo record service URL, used for material records + taxonomy dump
* OPENFORMAT_SERVICE_URL - OpenFormat record formatting service URL
* OPENNUMBERROLL_SERVICE_URL / OPENNUMBERROLL_NUMBERROLLNAME - FAUST number generation service
* FAUST_RESOLVER_URL - FAUST resolver service URL (required, no default in the app itself — only defaulted here in `scripts/common`)
* CULR_SERVICE_URL / CULR_SERVICE_USER_ID / CULR_SERVICE_PASSWORD - CULR user/reviewer sync service (default `none`, disables the integration)
* OAUTH2_CLIENT_ID / OAUTH2_CLIENT_SECRET - OAuth2 client credentials for token introspection against the login provider (dummy values by default; real auth needs real credentials)
* PROMAT_CLUSTER_NAME - Hazelcast cluster name. **Leave empty for local dev** (the default) — any non-empty value makes Payara try Hazelcast Kubernetes-DNS cluster discovery, which hangs forever outside a real k8s cluster. Only meaningful in the real staging/production deployment, which sets this independently via GitOps, not through these scripts.
* MAIL_HOST / MAIL_USER / MAIL_FROM / LU_MAILADDRESS - outgoing mail config (default `none`, disables mail sending)
* EMATERIAL_CONTENT_REPO - eMaterial content repo URL template
* PROMAT_AGENCY_ID - agency id Promat itself runs as (default `191977`)
* ENABLE_REMINDERS - enables the reminder batch job when set
* PROMAT_SKIP_MIGRATIONS - `true` starts the service without running Flyway migrations (local-dev only, see below)
* PROMAT_DISABLE_SCHEDULED_JOBS - `true` suppresses all scheduled batch jobs (case updates, reminders, taxonomy refresh, etc.)
* PROMAT_DISABLE_OUTBOUND_MAIL - `true` suppresses outbound email delivery

### API

The service exposes a RESTful [API](https://raw.githubusercontent.com/DBCDK/promat-service/master/service/docs/openapi.yaml). Once running locally, the interactive Swagger UI is available at `http://localhost:8080/v1/api/openapi-ui/`.

### Development

**Requirements**

To build this project, JDK 21 and Apache Maven are required (a `.sdkmanrc`
pins the JDK version if you use [SDKMAN](https://sdkman.io/)).

To start a local instance, Docker is required and must be running.

**IDE setup**

Get a JDK 21 onto your machine — [SDKMAN](https://sdkman.io) is the easiest
way: it installs per-shell, without touching your system Java or needing
sudo, which is nice if you're setting up a new machine or juggling several
projects' JDK versions at once. Not required though — any JDK 21 works, as
long as `JAVA_HOME` points at it.

```bash
sdk install java 21-tem
sdk use java 21-tem
java -version   # confirm it reports 21
```

Open the repository root in IntelliJ (`File → Open`) — it will detect the
multi-module Maven project (`connector/`, `model/`, `service/`). Then:

1. `File → Project Structure → Project` and set Project SDK to your JDK 21
   (point it at `~/.sdkman/candidates/java/21-tem` if using SDKMAN, or
   wherever else it lives, if IntelliJ doesn't detect it automatically).
2. Trigger a Maven re-import (the notification popup, or the refresh icon
   in the Maven tool window).

Unit tests and integration tests (`*IT.java`, which use TestContainers) can
be run directly from IntelliJ once the SDK is set — integration tests only
need Docker running, not `dev-start`. The Promat server itself always runs
in a Docker container via the scripts below, not as an IntelliJ run
configuration.

**Build the artifact**

```bash
./scripts/clean
mvn verify                    # builds the WAR and, via a bound plugin execution, the :devel Docker image
```
`mvn verify` from the repo root builds the whole multi-module reactor
(`connector`, `model`, `service`) and also produces the Docker image. If you
only want to rebuild the Docker image without re-running the full build, run
`../scripts/build docker` from inside `service/` (its `docker build` is
relative to that directory).

**Canonical local start**

With `.env.local` created and filled in (see Configuration above):

```bash
./scripts/dev-start
```

`./scripts/dev-start` loads `.env.local`, falls back to defaults from
`scripts/common`, builds the `:devel` image if it doesn't exist yet, starts
a local PostgreSQL container, and then starts Promat.

**Alternative start path**

```bash
./scripts/start-database
./scripts/start-server         # or ./scripts/start to do both in one step
```

Use this when you want to manage the database and app server separately.
`start-server` forwards any extra arguments straight into `docker run`, so
you can override any env var for a single run without editing tracked
scripts, e.g. `./scripts/start-server -e FAUST_RESOLVER_URL=http://my-override`.

**Using the staging database instead of a local, empty one**

Starting fresh with an empty local database means no reviewers, cases, or
taxonomy data to work against. To point the app at the real staging
database instead, set in `.env.local`:

```bash
PROMAT_DB_URL='<staging-user>:<staging-password>@<staging-host>:<staging-port>/<staging-dbname>'
PROMAT_SKIP_MIGRATIONS=true
```

`PROMAT_SKIP_MIGRATIONS=true` is important here — it stops Flyway from
running on startup, so your local server never applies schema migrations
against the shared staging database. Only run a new migration against
staging deliberately and after review, not as a side effect of starting
the app locally.

With this set, start only the app server — `./scripts/dev-start` and
`./scripts/start-database` both start (and expect) a *local* PostgreSQL
container, which you don't need here:

```bash
./scripts/start-server
```

**Scripts** (all under `scripts/`)
* `clean` - clears build artifacts
* `build [docker]` - `mvn verify` (builds artifacts, runs unit + integration tests); with `docker` arg, builds the local Docker image instead
* `validate` - analyzes source code and javadoc
* `start-database` - starts a local Postgres container
* `start-server` - starts the app server against the `:devel` Docker image, using `.env.local` plus defaults from `scripts/common`
* `start` - runs `start-database` then `start-server`
* `dev-start` - canonical local startup wrapper (builds the `:devel` image if it doesn't exist yet, then starts both containers)
* `dev-rebuild` - rebuilds the Docker image and restarts the local environment
* `stop` - stops both containers

Once up, the API is served at `http://localhost:8080/v1/api`.

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
