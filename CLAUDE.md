# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working rules

* This is a hiring take-home assignment. Treat the repo as production quality and follow standard GitHub practice.
* Ask the user before every `git push`. Never push without an explicit go-ahead in the same conversation.
* Do not commit placeholder, ambiguous, speculative or unverified content. Docs must describe what exists, not what is hoped for.
* Do not add repository collaborators until the user asks. The PRD lists `KartikWatts` and `Manasa-NL`, deferred for now.
* The owner must be able to explain every file in a follow-up discussion, so keep code simple and readable over clever.

## Current state

Planning stage. Only `docs/` and `README.md` exist; there is no build file or source yet. The commands below come from `docs/IMPLEMENTATION_PLAN.md` and are not verified until the Gradle project is created. Update this section as code lands.

Source of truth: `docs/BE_Assignment_PRD_Latest.md` (requirements) and `docs/IMPLEMENTATION_PLAN.md` (design, phases, test inventory).

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
