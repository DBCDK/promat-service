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

* PROMAT_DB_URL database URL (USER:PASSWORD@HOST:PORT/DBNAME) of the underlying promat database.
* OPENSEARCH_SERVICE_URL opensearch service url
* OPENSEARCH_PROFILE opensearch profile (optional, default is 'test')
* OPENSEARCH_AGENCY opensearch agency (optional, default is '100200')
* OPENSEARCH_REPOSITORY opensearch profile (optional, default is 'rawrepo_basis')
* WORK_PRESENTATION_PROFILE work-presentation profile (optional, default is 'test')
* PROMAT_SKIP_MIGRATIONS=true starts the service without Flyway migrations
* PROMAT_DISABLE_SCHEDULED_JOBS=true suppresses scheduled updates and taxonomy refresh jobs
* PROMAT_DISABLE_OUTBOUND_MAIL=true suppresses outbound email delivery

### API

The service exposes a RESTful [API](https://raw.githubusercontent.com/DBCDK/promat-service/master/service/docs/openapi.yaml).

### Development

**Requirements**

To build this project JDK 21 and Apache Maven is required.

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

**Canonical local start**

With `.env.local` created and filled in (see Configuration above):

```bash
./scripts/dev-start
```

`./scripts/dev-start` loads `.env.local`, falls back to defaults from
`scripts/common`, starts a local PostgreSQL container, and then starts Promat.

**Alternative start path**

```bash
./scripts/start-database
./scripts/start-server
```

Use this when you want to manage the database and app server separately.

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

**Scripts**
* clean - clears build artifacts
* build - builds artifacts and runs unit and integration tests
* validate - analyzes source code and javadoc
* start-database - starts local PostgreSQL
* start-server - starts the Promat application container using `.env.local` plus defaults from `scripts/common`
* dev-start - canonical local startup wrapper
* dev-rebuild - rebuilds the Docker image and restarts the local environment
* stop - stops local Promat containers

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
