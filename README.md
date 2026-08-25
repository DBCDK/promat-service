Promat service
==============

### Configuration

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

### API

The service exposes a RESTful [API](https://raw.githubusercontent.com/DBCDK/promat-service/master/service/docs/openapi.yaml). Once running locally, the interactive Swagger UI is available at `http://localhost:8080/v1/api/openapi-ui/`.

### Development

**Requirements**

To build this project, JDK 21 and Apache Maven are required (a `.sdkmanrc` pins the JDK version if you use [SDKMAN](https://sdkman.io/)). To start a local instance, Docker is also required.

**Scripts** (all under `scripts/`)
* `clean` - clears build artifacts
* `build [docker]` - `mvn verify` (builds artifacts, runs unit + integration tests); with `docker` arg, builds the local Docker image instead
* `validate` - analyzes source code and javadoc
* `start-database` - starts a local Postgres container
* `start-server` - starts the app server against the `:devel` Docker image built above
* `start` - runs `start-database` then `start-server`
* `stop` - stops both containers

**Quick start** (from the repo root, unless noted):
```bash
./scripts/clean
mvn verify                    # builds the WAR and, via a bound plugin execution, the :devel Docker image
./scripts/start-database
./scripts/start-server         # or ./scripts/start to do both in one step
```
Notes:
* `mvn verify` from the repo root builds the whole multi-module reactor (`connector`, `model`, `service`) and also produces the Docker image. If you only want to rebuild the Docker image without re-running the full build, run `../scripts/build docker` from inside `service/` (its `docker build` is relative to that directory).
* To override any env var for a single run without editing tracked scripts, pass an extra `-e` flag through — `scripts/start-server` forwards any extra arguments straight into `docker run`, e.g. `./scripts/start-server -e FAUST_RESOLVER_URL=http://my-override`.
* Once up, the API is served at `http://localhost:8080/v1/api`.

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
