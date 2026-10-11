# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working rules

* This is a hiring take-home assignment. Treat the repo as production quality and follow standard GitHub practice.
* Ask the user before every `git push`. Never push without an explicit go-ahead in the same conversation.
* Do not commit placeholder, ambiguous, speculative or unverified content. Docs must describe what exists, not what is hoped for.
* Do not change repository collaborators unless the user asks. The PRD lists `KartikWatts` and `Manasa-NL`. On 2026-10-11 GitHub showed `KartikWatts` as a collaborator and a pending invitation for `Manasa-NL`. Step 6.5 in `docs/STEPS.md` verifies both.
* The owner must be able to explain every file in a follow-up discussion, so keep code simple and readable over clever.
* Log every problem that costs real time, with cause and fix, in `docs/ISSUES.md`. Add the entry in the same change that resolves it.

## Current state

Phases 0 and 1 done (steps 0.1 to 1.4).

Build and run: Gradle skeleton (`settings.gradle.kts`, `build.gradle.kts` with Spring Boot 3.3.13, Java 21 toolchain, Spotless, Gradle 8.14.3 wrapper). `src/main/resources/application.yml` has `default` (PostgreSQL from env vars), `local` (PostgreSQL on localhost) and `test` (H2) profiles. `.gitignore` and `.env.example` exist; real secrets go in the git-ignored `.env`. Docker: multi-stage `Dockerfile`, `docker-compose.yml` (app plus PostgreSQL 16 with a healthcheck) and `.dockerignore`. `./gradlew build -x test spotlessCheck` passes on JDK 21.

Domain: `domain` package holds the four enums, `BaseEntity` (UUID id and audit fields) and the entities `User`, `Trek`, `Checkpoint`, `Participant`, `CheckIn`, `Alert`. `common/ClockConfig` provides the `Clock` bean and `config/JpaAuditingConfig` turns on auditing with that clock. `createdBy` stays empty until the auditor in step 2.4. Hibernate created all six tables on PostgreSQL 16 from these mappings.

Repositories: `repository` package has one Spring Data interface per entity. Notable queries: `AlertRepository.existsByParticipantIdAndCheckpointIdAndStatusIn` (idempotency guard, a check-then-insert that is safe on a single node), `CheckInRepository.existsByParticipantIdAndCheckpointId` (delay rule), `TrekRepository.findByStatus` and `ParticipantRepository.findByTrekIdAndStatus` (scheduler scope). `ParticipantRepository` also supports Specifications for the search API. Query names were validated by starting the app against PostgreSQL 16; their behavior has no tests yet.

Schema: `src/main/resources/db/migration/V1__init_schema.sql` creates the six tables with foreign keys, CHECK constraints on status values, unique constraints (username, trek and sequence number, participant and checkpoint for check-ins) and indexes. `ddl-auto` is `validate`, so Hibernate checks the entities against it at startup. `docker compose up --build` from a clean volume applies the migration and starts the app (HTTP 401 on port 8080). The same SQL ran without errors on H2 in PostgreSQL mode; the app itself has not run on H2 yet.

Seed data: `V2__seed_data.sql` adds users `admin` (ADMIN, password `Admin@12345`) and `ops` (OPERATIONS, password `Ops@12345`) stored as BCrypt hashes, the ACTIVE `Kedarkantha Trek` (start 2026-08-10 06:00 IST) with the six PRD checkpoints, and three participants (two `ON_TREK`, one `REGISTERED`). Because the trek started in the past, the delay scheduler of step 4.3 will raise alerts for both `ON_TREK` participants as soon as it runs. These demo passwords are for review only.

Next step: 2.1. The step list with interview lines is in `docs/STEPS.md`. Update "Current state" and tick steps there as code lands.

Source of truth: `docs/BE_Assignment_PRD_Latest.md` (requirements), `docs/IMPLEMENTATION_PLAN.md` (design, phases, test inventory), `docs/STEPS.md` (build order).

Build notes: `./gradlew` needs JDK 21 on `JAVA_HOME`. Maven Central rate-limits some environments (HTTP 429); retry, or use a mirror configured outside the repo.

## Planned commands

```bash
./gradlew build                                   # compile, test, jar
./gradlew test                                    # all tests
./gradlew test --tests '*DelayDetectionServiceTest'   # single test class
./gradlew spotlessCheck                           # lint (Spotless, google-java-format)
./gradlew spotlessApply                           # auto-fix formatting
docker compose up --build                         # app + PostgreSQL
```

## Architecture

Java 21, Spring Boot 3.3, Gradle Kotlin DSL, root package `com.indiahikes.trekops`. Layered: controller (thin, DTOs only) to service (all business rules) to Spring Data repository. Entities never leave the service layer.

Points that span several files:

* **Delay detection** (`DelayDetectionService`, called by a thin `DelayDetectionScheduler`): a participant is delayed at a checkpoint when `trek.startAt + expectedAfterHours` has passed and no `CheckIn` exists. Scan only `ACTIVE` treks and `ON_TREK` participants.
* **Alert idempotency**: at most one `OPEN` or `ACKNOWLEDGED` alert per participant and checkpoint, checked inside a transaction. Re-running the scheduler must not create duplicates.
* **Time**: inject a `java.time.Clock` bean everywhere. Never call `Instant.now()` directly, or the scheduler cannot be tested with a fixed clock.
* **Notifications**: alert creation publishes `AlertCreatedEvent`; `NotificationDispatcher` fans out to `NotificationChannel` implementations. Adding a channel means adding one class.
* **Security**: stateless JWT, roles `ADMIN` and `OPERATIONS`. Only ADMIN creates treks, checkpoints and changes trek status. All endpoints except `/api/auth/login` and `/api/health` need a token.
* **Errors**: one `ApiError` shape from a single `@RestControllerAdvice`. Do not return ad hoc error bodies.
* **Persistence**: schema through Flyway (`V1__init_schema.sql`, `V2__seed_data.sql`), PostgreSQL at runtime, H2 in tests, so avoid DB specific SQL. Audit fields via JPA auditing.
* **Config**: scheduler interval and toggle, JWT secret and TTL, and notification channels come from environment variables. Secrets go in git-ignored `.env`; commit only `.env.example`.

## Required deliverables

`README.md`, `DOMAIN_MODEL.md`, `DECISIONS.md`, `SELF_REVIEW.md`, `TEST_NOTES.md` at the repo root, plus AI session logs. They must match the code. `SELF_REVIEW.md` has to say which AI-assisted parts were reviewed carefully.
