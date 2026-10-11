# indiahikes-backend-assignment-ArushNaudiyal

Backend for the Trek Operations Platform take-home assignment. It manages treks, their checkpoints, participants and check-ins. A background job detects participants who have not reported at a checkpoint in time, raises alerts, and notifies the operations team. The API is secured with JWT and two roles.

Java 21, Spring Boot 3.3, Gradle (Kotlin DSL), PostgreSQL 16, Flyway, Spring Security with JWT, springdoc-openapi, JUnit 5 and Mockito, Docker Compose.

## Documents

| Document | Contents |
|---|---|
| [DOMAIN_MODEL.md](DOMAIN_MODEL.md) | Entities, relationships, state transitions, the delay rule, constraints |
| [DECISIONS.md](DECISIONS.md) | Design decisions and trade-offs |
| [TEST_NOTES.md](TEST_NOTES.md) | Testing strategy, commands, how to test the APIs, limitations |
| [SELF_REVIEW.md](SELF_REVIEW.md) | Strengths, weaknesses, limitations, next steps, AI assistance |
| [docs/BE_Assignment_PRD_Latest.md](docs/BE_Assignment_PRD_Latest.md) | The assignment |
| [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) | The original design plan |
| [docs/STEPS.md](docs/STEPS.md) | Build order, one step per commit |
| [docs/ISSUES.md](docs/ISSUES.md) | Problems found during the build and how they were fixed |

## Setup

You need:

* Docker with Docker Compose, for the single-command run.
* JDK 21, only for building, testing or running outside Docker. The Gradle wrapper downloads Gradle itself.

Create your environment file from the template, then set the two secrets:

```bash
cp .env.example .env
```

* `JWT_SECRET` must be at least 32 characters, for example `openssl rand -base64 48`.
* `DB_PASSWORD` can be any password you choose.

`.env` is git-ignored. Never commit it.

## Run with Docker Compose

```bash
docker compose up --build
```

This builds the app image, starts PostgreSQL 16, waits for its health check, then starts the app. On first start, Flyway creates the schema and loads the seed data.

| What | Where |
|---|---|
| API | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health | `http://localhost:8080/api/health` (public) |
| PostgreSQL | `127.0.0.1:5432`, database and user `trekops`, password from `.env` |

To stop, run `docker compose down`. Add `-v` to also delete the database volume.

## Build

```bash
./gradlew build
```

This compiles the code, runs all tests, runs the Spotless formatting check and builds the jar in `build/libs/`.

## Run locally

Use this to run the app from your IDE or Gradle against the Compose database:

```bash
docker compose up -d db
DB_PASSWORD=<value from .env> ./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile connects to PostgreSQL on `localhost:5432` and uses a development JWT secret. Stop the Compose `app` container first if it is running, because both use port 8080.

## Authentication

* Log in with `POST /api/auth/login` and a body of `{"username": "...", "password": "..."}`.
* The response holds a JWT. Send it on every other request as `Authorization: Bearer <token>`.
* Tokens last 480 minutes by default. Set `TOKEN_TTL_MINUTES` to change that.
* In Swagger UI, call login, then paste the token into **Authorize**.

| Role | Can do |
|---|---|
| `ADMIN` | Everything, including creating treks and checkpoints and changing trek status |
| `OPERATIONS` | Read treks, participants and alerts; register participants and change their status; record check-ins; acknowledge and resolve alerts |

`/api/auth/login`, `/api/health` and the Swagger pages are public. Every other endpoint needs a token.

## API overview

| Method and path | Role | Purpose |
|---|---|---|
| `POST /api/auth/login` | public | Get a token |
| `GET /api/health` | public | App name, version, status, database state |
| `GET /api/treks` | both | List treks (paged) |
| `POST /api/treks` | ADMIN | Create a trek |
| `GET /api/treks/{id}` | both | Trek with its checkpoints |
| `PATCH /api/treks/{id}/status` | ADMIN | Change trek status |
| `POST /api/treks/{id}/checkpoints` | ADMIN | Add a checkpoint |
| `GET /api/participants` | both | Search: `trekId`, `status`, `name`, `phone`, plus `page`, `size`, `sort` |
| `POST /api/participants` | both | Register a participant |
| `GET /api/participants/{id}` | both | Participant with check-ins |
| `PATCH /api/participants/{id}/status` | both | Change participant status |
| `POST /api/check-ins` | both | Record a check-in |
| `GET /api/alerts?status=` | both | List alerts (paged, newest first) |
| `POST /api/alerts/{id}/acknowledge` | both | Acknowledge an alert |
| `POST /api/alerts/{id}/resolve` | both | Resolve an alert |

Every error returns the same JSON shape: `timestamp`, `status`, `error`, `message`, `path`, plus `errors` for validation failures.

## Configuration

All settings come from environment variables. Docker Compose reads them from `.env`.

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | none, required | Token signing key, at least 32 characters |
| `DB_USER` / `DB_PASSWORD` | `trekops` / required | Database credentials |
| `TOKEN_TTL_MINUTES` | `480` | Token lifetime |
| `DELAY_CHECK_ENABLED` | `true` | Turns the delay check on or off |
| `DELAY_CHECK_INTERVAL_MS` | `60000` | Pause between delay checks |
| `DELAY_CHECK_GRACE_MINUTES` | `0` | Minutes after the expected time before a participant counts as late |
| `NOTIFICATION_CHANNELS` | `log` | Comma-separated notification channels |

Profiles:

* `default` is PostgreSQL from the variables above, used by Docker.
* `local` is PostgreSQL on localhost.
* `test` is in-memory H2 with the delay check off.

## Tests

```bash
./gradlew test                                      # all 155 tests, no Docker needed
./gradlew test --tests '*DelayDetectionServiceTest' # one class
./gradlew spotlessCheck                             # formatting
```

The tests run on H2 with the real Flyway migrations. See [TEST_NOTES.md](TEST_NOTES.md) for the strategy and for manual API checks.

## Seed data

Flyway migration `V2__seed_data.sql` loads:

| What | Details |
|---|---|
| Administrator | `admin` / `Admin@12345`, role `ADMIN` |
| Operations user | `ops` / `Ops@12345`, role `OPERATIONS` |
| Trek | `Kedarkantha Trek`, `ACTIVE`, starts 2026-08-10 06:00 IST |
| Checkpoints | Dehradun 0 h, Sankri 8 h, Juda Ka Talab 28 h, Kedarkantha Summit 48 h, Sankri 72 h, Dehradun 84 h |
| Participants | Aarav Sharma and Diya Mehta (`ON_TREK`), Rohan Verma (`REGISTERED`) |

Passwords are stored as BCrypt hashes. These are demo credentials; change them before any real use.

The seed trek started in the past and has no check-ins. The first delay check after startup therefore raises 12 alerts, one per checkpoint for each `ON_TREK` participant. You can see them at `GET /api/alerts`, and as `ALERT` lines in the app log.

## Project layout

```
src/main/java/com/indiahikes/trekops/
  config/        security, JWT filter and properties, auditing, OpenAPI, scheduling
  domain/        JPA entities and enums
  repository/    Spring Data repositories and search specifications
  service/       business rules, delay detection, login, health
  scheduler/     the scheduled delay check
  notification/  alert event, channels, dispatcher
  exception/     exceptions mapped to 404, 409, 422 and 400
  web/           controllers, DTOs, mappers, error handling
src/main/resources/
  application.yml
  db/migration/  V1__init_schema.sql, V2__seed_data.sql
```

## AI assistance

This project was built with Claude Code, step by step, under the working rules in [CLAUDE.md](CLAUDE.md). [SELF_REVIEW.md](SELF_REVIEW.md) describes how the AI was used and checked.
