# Build steps

Build order for the backend. Each step is one reviewable change. Tick a step only after its code compiles, its tests pass where they exist, and the owner can explain it.

Decisions already made:
- Java 21, Spring Boot 3.3.13, Gradle 8.14.3 wrapper, Gradle Kotlin DSL.
- Spotless with google-java-format. Lint command: `./gradlew spotlessCheck`.
- PostgreSQL 16 at runtime, H2 in tests, Flyway migrations.
- Stateless JWT, roles `ADMIN` and `OPERATIONS`.
- `@Scheduled` for delay detection, single node only.
- `Clock` bean injected everywhere.

Workflow for every step: explain what and why, read the diff, answer questions, then commit locally. Ask before every push.

## Phase 0: Setup

- [x] **0.1 Toolchain check.** JDK 21, Docker, Compose, git.
  Be able to say: why JDK 21 and why Docker are required.
- [x] **0.2 Gradle skeleton.** `settings.gradle.kts`, `build.gradle.kts`, wrapper, dependencies, Java 21 toolchain, Spotless, `TrekOpsApplication`.
  Be able to say: what each dependency does, why Kotlin DSL, why `jjwt-api` is compile-scoped while `impl` and `jackson` are runtime-only.
- [x] **0.3 Configuration.** `application.yml` with profiles (`default`, `test`, `local`) and environment variables.
  Be able to say: how profiles and environment variables give multi-environment config.
- [x] **0.4 Ignore and env files.** `.gitignore` (build output, `.gradle/`, `.idea/`, `.env`) and `.env.example`.
  Be able to say: how secrets stay out of git.
- [x] **0.5 Docker.** Multi-stage `Dockerfile` and `docker-compose.yml` with a PostgreSQL healthcheck and `depends_on`.
  Be able to say: why multi-stage, and why `depends_on` needs a healthcheck.
- [x] **0.6 Verify.** `docker compose up --build` starts the empty app. `./gradlew build spotlessCheck` passes.
  Be able to say: what proves the empty app starts.

## Phase 1: Domain and persistence

- [x] **1.1 Enums and entities.** `User`, `Trek`, `Checkpoint`, `Participant`, `CheckIn`, `Alert`, with JPA auditing.
  Be able to say: the ER diagram, why UUIDs, what the audit fields track.
- [x] **1.2 Repositories.** Including the alert idempotency query.
  Be able to say: how duplicate alerts are prevented.
- [x] **1.3 Flyway V1 schema.** Constraints, including the unique check-in per participant and checkpoint.
  Be able to say: why migrations, and why the unique constraint.
- [x] **1.4 Flyway V2 seed.** Admin user, Kedarkantha trek with the PRD checkpoints, sample participants.
  Be able to say: which credentials are seeded and how they are stored.

## Phase 2: Security

- [x] **2.1 JwtService.** Issue and parse tokens, HS256, secret and TTL from config.
  Be able to say: JWT structure, signing, expiry.
- [x] **2.2 Filter and SecurityConfig.** Stateless filter chain and role rules.
  Be able to say: the filter chain order and which endpoints are public.
- [ ] **2.3 AuthController and BCrypt.** Login flow.
  Be able to say: the login flow, and why passwords are hashed.
- [ ] **2.4 AuditorAware.** Fills created-by from the JWT principal, falls back to `system`.
  Be able to say: how created-by gets filled in.

## Phase 3: Core APIs

- [ ] **3.1 DTOs, mappers, errors.** Request and response DTOs, request validation with Bean Validation annotations, `ApiError` record and the global exception handler. Validation failures return 400 with the failing fields.
  Be able to say: why DTOs, where validation happens, and why one error shape.
- [ ] **3.2 Treks and checkpoints.** Create, read, status change, role restrictions.
  Be able to say: which role can do what, and why.
- [ ] **3.3 Participants and search.** Register participant, participant detail with check-ins, status update (the way a participant becomes `ON_TREK`), and the search with Specifications, pagination and a sort whitelist.
  Be able to say: how a participant becomes `ON_TREK` and why the scheduler needs that, how filters compose, and why sort fields are whitelisted.
- [ ] **3.4 Check-in.** Trek match, duplicate check, timestamp default.
  Be able to say: the three validation rules and their status codes (422, 409, 201).
- [ ] **3.5 Health and Swagger.** Public health endpoint, JWT in Swagger UI.
  Be able to say: what is public and how the token is sent from Swagger.

## Phase 4: Scheduler, alerts, notifications

- [ ] **4.1 Alert endpoints.** List, acknowledge, resolve, and the state machine.
  Be able to say: the allowed transitions.
- [ ] **4.2 DelayDetectionService.** Injected `Clock`, the delay rule.
  Be able to say: the delay rule, and why the clock is injected.
- [ ] **4.3 Scheduler.** `@Scheduled` with config toggle and interval.
  Be able to say: why `@Scheduled`, and what breaks with multiple nodes.
- [ ] **4.4 Notifications.** `AlertCreatedEvent`, `NotificationChannel`, log channel.
  Be able to say: how an email channel would be added.

## Phase 5: Tests

- [ ] **5.1 Service tests.** Delay logic, idempotency, check-in rules, alert transitions.
  Be able to say: what each test proves, and why it uses mocks or a fixed clock.
- [ ] **5.2 Controller tests.** RBAC, validation, search, error shape.
  Be able to say: what a `@WebMvcTest` slice covers and what it does not.
- [ ] **5.3 Integration smoke test.** Login and Flyway migration against H2.
  Be able to say: what it catches that unit tests do not.

## Phase 6: Docs and finish

- [ ] **6.1 DOMAIN_MODEL.md and DECISIONS.md.**
  Be able to say: each entity, state, constraint, and the trade-off behind each choice.
- [ ] **6.2 SELF_REVIEW.md and TEST_NOTES.md.**
  Be able to say: the weaknesses, the limits, and which AI-assisted parts were reviewed hardest.
- [ ] **6.3 Final README and AI session logs.**
  Be able to say: a stranger can run the project from the README alone.
- [ ] **6.4 Clean checkout test.** Clone fresh, run `docker compose up --build`.
  Be able to say: the project runs on a machine that has nothing else installed.
- [ ] **6.5 Collaborators.** Confirm the repository is private and that `KartikWatts` and `Manasa-NL` are collaborators (`gh api repos/<owner>/<repo>/collaborators` and `.../invitations`). Ask the owner before changing anything.
  Be able to say: who has access and why the repository stays private.
