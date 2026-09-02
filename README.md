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

To start a local instance, docker is required.

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
