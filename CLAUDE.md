# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Promat Service is a Danish Library Center (DBC) microservice managing literary review/evaluation tasks. It coordinates case assignment to reviewers, tracks workflow states, sends notifications, and integrates with multiple external bibliographic services. It incorporates functionality from the former "Metakompas" system.

**Stack:** Java 21, Jakarta EE (JAX-RS, JPA), Payara 6 Micro, PostgreSQL, Flyway

## Modules

Multi-module Maven project:
- `connector/` — HTTP client library for external consumers of Promat Service
- `model/` — Shared JPA entities, DTOs, persistence definitions
- `service/` — Main REST API WAR, batch jobs, database migrations

## Build & Test Commands

```bash
mvn verify               # Build + all tests (unit + integration)
mvn test                 # Unit tests only
mvn -Dtest=ClassName test          # Single unit test class
mvn -Dit.test=ClassNameIT verify   # Single integration test class
./scripts/build          # Equivalent to mvn verify
./scripts/build docker   # Build + Docker image
```

Integration tests (`*IT.java`) use TestContainers (spins up PostgreSQL + Payara containers) and WireMock for external service mocking.

## Local Development

```bash
./scripts/start-database    # Start PostgreSQL container
./scripts/start-server      # Start Payara app server
./scripts/start             # Start both
./scripts/stop              # Stop both
```

Environment is configured via `scripts/common` (defaults) or `.env.local` (local overrides). Required env vars: `PROMAT_DB_URL`, `OPENSEARCH_SERVICE_URL`, `RECORD_SERVICE`, `OPENFORMAT_SERVICE_URL`, `OPENNUMBERROLL_SERVICE_URL`, `CULR_SERVICE_URL`.

## Architecture

### REST API

Base path: `/v1/api` (defined in `PromatApplication.java`)

Key resource classes in `service/src/main/java/dk/dbc/promat/service/api/`:
- `Cases.java` — Case CRUD, search, filtering
- `Tasks.java` — Task management within cases
- `Reviewers.java`, `Editors.java`, `Users.java` — User management
- `Subjects.java` — Subject/taxonomy hierarchy
- `Records.java` — Bibliographic record lookups
- `Messages.java`, `Payments.java` — Communication and payment tracking
- `Batch.java` — Batch job triggers
- `TaxonomyService.java` — Metakompas taxonomy API

### Core Domain

- **PromatCase** — Central entity; workflow: `CREATED → ASSIGNED → APPROVED → EXPORTED → CLOSED/DONE`
- **PromatTask** — Work item within a case (types: BRIEF, DESCRIPTION, EVALUATION, COMPARISON, RECOMMENDATION, TOPICS, BKM, EXPRESS, METAKOMPAS, AGE, MATLEVEL, BUGGI)
- **Reviewer** — Expert with subject expertise, availability (schedule), types of material he/she accepts for review. Basic data: Address, phone, email. 
- **Subject** — Hierarchical taxonomy item; parent-child relationships
- **Notification** — Pending emails (rendered from JTE templates). Mostly used for recap on accepted case assigments, and for sending reminders on due/overdue reviews.

Entities are in `model/src/main/java/dk/dbc/promat/service/persistence/`.

### Batch Jobs

Singleton EJBs in `service/src/main/java/dk/dbc/promat/service/batch/`:
- `ScheduledCaseInformationUpdater` — Updates case data from external sources (every 10 min, 6am–6pm weekdays)
- `ScheduledReminders` — Sends deadline reminder emails
- `ScheduledNotificationSender` — Flushes notification queue (every 2 min)
- `ScheduledUserUpdater` — Remove sensible data such as address, phone and email from users that has been deactivated.

**Cluster awareness:** Jobs only run on `PRIMARY` server (via `ServerRole`/Hazelcast) to prevent duplicate execution.

### Taxonomy Service

`TaxonomyService` fetches all records from agency `190004` via rawrepo record service, builds a hierarchical tree for Metakompas subject classification. Cache refreshes hourly. See `service/src/main/java/dk/dbc/promat/service/taxonomy/README.md` for tree structure.

### Database

- **JNDI:** `jdbc/promat`
- **Migrations:** Flyway, located in `service/src/main/resources/dk/dbc/promat/service/db/migration/`
- **Persistence units:** `promatPU` (JTA, production) and `promatITPU` (RESOURCE_LOCAL, integration tests)

### External Integrations

| Service | Purpose |
|---------|---------|
| OpenSearch | Bibliographic data lookup |
| OpenFormat | Record formatting |
| OpenNumberRoll | FAUST number generation |
| CULR | User/reviewer data sync |
| Rawrepo Record Service | Material records + taxonomy dump |

### Testing Infrastructure

- `IntegrationTestIT` / `ContainerTest` — Base classes for integration tests
- `AuthMocks` — OAuth mocking
- WireMock stubs in `service/src/test/resources/__files/`
- Integration tests use `promatServiceConnector` (the connector module) to call the running test service

## Deployment

Docker image: `docker-metascrum.artifacts.dbccloud.dk/promat-service`
Base image: `docker-dbc.artifacts.dbccloud.dk/payara6-micro:latest`
CI/CD: Jenkinsfile — builds on master, pushes Docker image, updates staging GitOps config, deploys model/connector JARs to DBC Maven repo.