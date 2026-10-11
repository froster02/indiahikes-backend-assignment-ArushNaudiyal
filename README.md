# indiahikes-backend-assignment-ArushNaudiyal

Backend for the Trek Operations Platform take-home assignment. It tracks treks, checkpoints, participants and check-ins, detects delayed participants on a schedule, and raises alerts for the operations team.

## Status

Early build. Phase 0 (Gradle skeleton, configuration profiles, Docker files) and the domain entities are committed. There are no APIs, no Flyway migrations and no tests yet, so `docker compose up --build` fails at startup until the schema migration lands. Sections below describe the intended setup and get filled in with verified commands as each phase lands. Build order and progress are in [docs/STEPS.md](docs/STEPS.md).

Documents:

* [Assignment PRD](docs/BE_Assignment_PRD_Latest.md)
* [Implementation plan and architecture](docs/IMPLEMENTATION_PLAN.md)

## Stack

* Java 21, Spring Boot 3.3, Gradle (Kotlin DSL)
* PostgreSQL 16 at runtime, H2 for tests, Flyway migrations
* Spring Security with stateless JWT, roles `ADMIN` and `OPERATIONS`
* springdoc-openapi (Swagger UI)
* JUnit 5, Mockito, AssertJ
* Docker and Docker Compose

## Planned architecture

Layered Controller, Service, Repository, with DTOs between the API and JPA entities.

* Delay detection runs as a `@Scheduled` job. A participant is delayed at a checkpoint when the trek start plus the checkpoint offset has passed and no check-in exists. Only `ACTIVE` treks and `ON_TREK` participants are scanned.
* Alerts are idempotent per participant and checkpoint: at most one `OPEN` or `ACKNOWLEDGED` alert at a time.
* Alert creation publishes an event. A `NotificationChannel` interface with a log implementation makes new channels a one class change.
* All errors share one `ApiError` shape. Entities carry created and updated audit fields.

## Setup

Planned prerequisites: JDK 21, Docker with Compose. Filled in once the project skeleton exists.

## Build

Planned:

```bash
./gradlew build
```

## Run locally

Planned: run with the `local` profile against H2 or a local PostgreSQL. Commands to be added.

## Run with Docker Compose

Planned single command:

```bash
docker compose up --build
```

## Authentication

Planned: `POST /api/auth/login` returns a JWT, sent as a Bearer token. Swagger UI will be available on the app port. Secrets come from environment variables, with `.env.example` committed and `.env` ignored.

## Tests

Planned:

```bash
./gradlew test
```

## Lint

Planned: Spotless with google-java-format.

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

## Seed data

Planned: one administrator user and the sample Kedarkantha trek with its six checkpoints and a few participants. Credentials will be documented here once the migration exists.

## Deliverables

| File | Status |
|------|--------|
| README.md | In progress |
| DOMAIN_MODEL.md | Not started |
| DECISIONS.md | Not started |
| SELF_REVIEW.md | Not started |
| TEST_NOTES.md | Not started |

AI assistance is used on this project. Session logs will be included as the PRD asks.
