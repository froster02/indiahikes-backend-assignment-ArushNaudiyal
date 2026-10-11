# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working rules

* This is a hiring take-home assignment. Treat the repo as production quality and follow standard GitHub practice.
* Ask the user before every `git push`. Never push without an explicit go-ahead in the same conversation.
* Do not commit placeholder, ambiguous, speculative or unverified content. Docs must describe what exists, not what is hoped for.
* Do not change repository collaborators unless the user asks. The PRD lists `KartikWatts` and `Manasa-NL`. On 2026-10-11 GitHub showed `KartikWatts` as a collaborator and a pending invitation for `Manasa-NL`. Step 6.5 in `docs/STEPS.md` verifies both.
* The owner must be able to explain every file in a follow-up discussion, so keep code simple and readable over clever.
* Log every problem that costs real time, with cause and fix, in `docs/ISSUES.md`. Add the entry in the same change that resolves it.
* Before every step, read the matching sections of `docs/BE_Assignment_PRD_Latest.md` and check the planned work against them. If the step or the plan does not match the PRD, flag it to the user before changing anything.
* After every change, check that the whole project still works: `./gradlew build` with all tests, then `docker compose up --build` and a login request. Report anything broken before asking for a commit.

## Current state

Phases 0, 1 and 2 done (steps 0.1 to 2.4) and steps 3.1 and 3.2 done.

Build and run: Gradle skeleton (`settings.gradle.kts`, `build.gradle.kts` with Spring Boot 3.3.13, Java 21 toolchain, Spotless, Gradle 8.14.3 wrapper). `src/main/resources/application.yml` has `default` (PostgreSQL from env vars), `local` (PostgreSQL on localhost) and `test` (H2) profiles. `.gitignore` and `.env.example` exist; real secrets go in the git-ignored `.env`. Docker: multi-stage `Dockerfile`, `docker-compose.yml` (app plus PostgreSQL 16 with a healthcheck; the database is published on `127.0.0.1:5432` for local DB clients) and `.dockerignore`. `./gradlew build -x test spotlessCheck` passes on JDK 21.

Domain: `domain` package holds the four enums, `BaseEntity` (UUID id and audit fields) and the entities `User`, `Trek`, `Checkpoint`, `Participant`, `CheckIn`, `Alert`. `common/ClockConfig` provides the `Clock` bean and `config/JpaAuditingConfig` turns on auditing with that clock. `config/SecurityAuditorAware` fills `createdBy` and `updatedBy` with the JWT username of the current request, or `system` when nobody is logged in. Hibernate created all six tables on PostgreSQL 16 from these mappings.

Repositories: `repository` package has one Spring Data interface per entity. Notable queries: `AlertRepository.existsByParticipantIdAndCheckpointIdAndStatusIn` (idempotency guard, a check-then-insert that is safe on a single node), `CheckInRepository.existsByParticipantIdAndCheckpointId` (delay rule), `TrekRepository.findByStatus` and `ParticipantRepository.findByTrekIdAndStatus` (scheduler scope). `ParticipantRepository` also supports Specifications for the search API. Query names were validated by starting the app against PostgreSQL 16; their behavior has no tests yet.

Schema: `src/main/resources/db/migration/V1__init_schema.sql` creates the six tables with foreign keys, CHECK constraints on status values, unique constraints (username, trek and sequence number, participant and checkpoint for check-ins) and indexes. `ddl-auto` is `validate`, so Hibernate checks the entities against it at startup. `docker compose up --build` from a clean volume applies the migration and starts the app (HTTP 401 on port 8080). The same migrations run on H2 in PostgreSQL mode under the `test` profile (`AuditingRepositoryTest`); the H2 URL must not set `DATABASE_TO_UPPER=false`, see `docs/ISSUES.md` entry 5.

Seed data: `V2__seed_data.sql` adds users `admin` (ADMIN, password `Admin@12345`) and `ops` (OPERATIONS, password `Ops@12345`) stored as BCrypt hashes, the ACTIVE `Kedarkantha Trek` (start 2026-08-10 06:00 IST) with the six PRD checkpoints, and three participants (two `ON_TREK`, one `REGISTERED`). Because the trek started in the past, the delay scheduler of step 4.3 will raise alerts for both `ON_TREK` participants as soon as it runs. These demo passwords are for review only.

JWT: `config/JwtProperties` binds `app.security.jwt-secret` and `token-ttl-minutes` and validates them at startup (secret required, at least 32 characters), so the app stops if `JWT_SECRET` is missing or short. `service/JwtService` issues HS256 tokens (subject is the username, `role` claim, issued-at and expiry from the injected `Clock`) and parses them, throwing `JwtException` for bad, tampered or expired tokens. `JwtServiceTest` has 5 passing tests. 
Security: `config/JwtAuthFilter` reads the Bearer token and authenticates that request only (authority `ROLE_ADMIN` or `ROLE_OPERATIONS`, principal is the username, no database lookup). `config/SecurityConfig` is stateless with CSRF, form login and basic auth off. Public: `POST /api/auth/login`, `/api/health`, `/error` (so clients see real 400 and 404 statuses), Swagger and API-doc paths. ADMIN only: `POST /api/treks`, `POST /api/treks/*/checkpoints`, `PATCH /api/treks/*/status`. Everything else needs any valid token, including alert acknowledge and resolve for both roles, following the plan's role matrix. Security rejections (401, 403) return the `ApiError` body through `config/SecurityErrorHandler`. `SecurityConfigTest` (7 tests, throwaway controller) covers the rules.

Login: `POST /api/auth/login` (`web/AuthController`, `web/dto/LoginRequest` and `LoginResponse`) returns `token`, `tokenType` (`Bearer`) and `expiresInSeconds`. `service/AuthService` loads the user, checks the BCrypt hash (`PasswordEncoder` bean in `SecurityConfig`) and throws `BadCredentialsException` with one message for unknown user and wrong password, running a dummy hash comparison for unknown users to even out timing. Spring's generated default user is excluded in `TrekOpsApplication`. 55 tests pass (`SecurityConfigTest`, `JwtServiceTest`, `AuthServiceTest`, `AuthControllerTest`, `SecurityAuditorAwareTest`, `AuditingRepositoryTest`, `GlobalExceptionHandlerTest`, `TrekServiceTest`, `TrekControllerTest`). Checked live in Docker with the seeded users: admin and ops log in, wrong password and unknown user give 401, blank fields give 400, and an ops token gets 403 on `POST /api/treks`. No login rate limiting. A bad login returns 401 with the `ApiError` body.

Errors: `web/ApiError` is the one error body (`timestamp`, `status`, `error`, `message`, `path`, and an `errors` map of field to message for validation failures). `web/GlobalExceptionHandler` maps validation and malformed requests to 400, bad credentials to 401, access denied to 403, unknown paths and `ResourceNotFoundException` to 404, wrong method to 405, `ConflictException` to 409, `BusinessRuleException` to 422, and anything unexpected to a generic 500 with the detail only in the log. Services throw the three exceptions in the `exception` package. Participant, check-in and alert DTOs and mappers are built with their endpoints in steps 3.3, 3.4 and 4.1. An unknown sort property gives 400, and page size is capped at 100 (`spring.data.web.pageable.max-page-size`). Checked live in Docker: every error path returns the JSON shape.

Treks: `web/TrekController` with `GET /api/treks` (paged `PageResponse`, default sort `startAt`), `POST /api/treks` (201, `Location`), `GET /api/treks/{id}` (trek plus checkpoints in sequence order), `PATCH /api/treks/{id}/status` and `POST /api/treks/{id}/checkpoints`. `service/TrekService` returns DTOs through `web/mapper/TrekMapper`, so entities never reach controllers. Rules: status PLANNED to ACTIVE or CANCELLED, ACTIVE to COMPLETED or CANCELLED, COMPLETED and CANCELLED final; ACTIVE needs at least one checkpoint; checkpoints must fall inside the trek and expected hours must grow with sequence (422); a used sequence number is 409; finished treks take no checkpoints. Responses carry audit fields. Checked live: a trek created by admin stores `created_by = admin`.

Next step: 3.3. The step list with interview lines is in `docs/STEPS.md`. Update "Current state" and tick steps there as code lands.

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
