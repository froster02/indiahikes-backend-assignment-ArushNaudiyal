# Test notes

## Setup

You need JDK 21 to build and run tests, and Docker with Compose to run the full stack. Nothing else is required. Gradle comes with the wrapper.

```bash
cp .env.example .env
# Fill in JWT_SECRET (at least 32 characters) and DB_PASSWORD, for example:
#   JWT_SECRET=$(openssl rand -base64 48)
docker compose up --build
```

The app listens on `http://localhost:8080`. PostgreSQL is published on `127.0.0.1:5432` for database clients. Flyway creates the schema and loads the seed data on first start.

## Commands

```bash
./gradlew test                                      # all 155 tests (H2, no Docker needed)
./gradlew test --tests '*DelayDetectionServiceTest' # one test class
./gradlew build                                     # tests, Spotless check and jar
./gradlew spotlessCheck                             # formatting check (google-java-format)
./gradlew spotlessApply                             # fix formatting
docker compose up --build                           # app and PostgreSQL
docker compose down                                 # stop; add -v to also delete the database
```

## Testing strategy

All tests use JUnit 5, Mockito and AssertJ. They run against H2 in PostgreSQL mode with the real Flyway migrations, so no Docker is needed. The scheduler is switched off in the `test` profile.

| Layer | Tests | What they prove |
|---|---|---|
| Service unit tests (Mockito) | 64 | Every business rule: the delay rule, alert idempotency, check-in validation, every state transition, login, JWT, health. Repositories are mocked. Time-dependent tests use `Clock.fixed`, so "late" is tested at exact moments. |
| Controller slices (`@WebMvcTest`) | 56 | HTTP behavior with the real security config and error handler: 401 without a token, 403 for the wrong role, 400 with field errors, response shapes, paging and sort parameters, and the `ApiError` body for every error type. Services are mocked. |
| Repository and H2 tests (`@DataJpaTest`) | 21 | Real SQL on H2 against the seed data: the search filters, the unique check-in constraint, alert queries, auditing, and the full delay rule (4 alerts for the seed trek, none on a rerun). |
| Other unit tests | 10 | The JWT auditor, the notification dispatcher (channel choice, failure isolation), and the scheduler on and off switch. |
| Full application (`@SpringBootTest`) | 4 | The whole app on its own H2 database: both migrations apply, the seed users log in with BCrypt and their roles, and one flow runs through every layer. The flow covers check-in, duplicate, the delay check, the notification after commit, and acknowledge and resolve. |

Why this split: the business rules carry the risk, so they get the most tests and run fast with mocks. Slices check the HTTP contract without a database. A few H2 tests catch what mocks cannot, such as wrong SQL, missing constraints and entity and schema mismatches. One full test proves the pieces are wired together.

Every feature was also checked by hand against the Docker stack with PostgreSQL, using curl, before it was committed.

## How to test the APIs

### Seed data

| What | Value |
|---|---|
| Admin user | `admin` / `Admin@12345` (role `ADMIN`) |
| Operations user | `ops` / `Ops@12345` (role `OPERATIONS`) |
| Trek | `Kedarkantha Trek`, id `b0000000-0000-0000-0000-000000000001`, `ACTIVE`, starts 2026-08-10 06:00 IST |
| Checkpoints | ids `c0000000-0000-0000-0000-00000000000{1..6}`: Dehradun 0 h, Sankri 8 h, Juda Ka Talab 28 h, Kedarkantha Summit 48 h, Sankri 72 h, Dehradun 84 h |
| Participants | Aarav Sharma `d0…01` and Diya Mehta `d0…02` are `ON_TREK`; Rohan Verma `d0…03` is `REGISTERED` |

The seed trek started in the past and is `ACTIVE`, with no check-ins. The scheduler's first run after startup therefore raises 12 alerts, one per checkpoint for each of the two `ON_TREK` participants. This shows the delay detection without any setup.

### Swagger UI

1. Open `http://localhost:8080/swagger-ui/index.html`.
2. Run `POST /api/auth/login` with `{"username":"ops","password":"Ops@12345"}` and copy the `token`.
3. Click **Authorize**, paste the token and confirm.
4. Every other endpoint now sends the token. Health and login need none.

### curl walk-through

```bash
U=http://localhost:8080
TOKEN=$(curl -s -X POST $U/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"ops","password":"Ops@12345"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
H="Authorization: Bearer $TOKEN"

curl -s $U/api/health                                         # public
curl -s -H "$H" "$U/api/participants?status=ON_TREK&sort=name,asc"
curl -s -H "$H" "$U/api/alerts?status=OPEN"                   # alerts raised by the scheduler

# Record that Rohan reached Dehradun (time defaults to now)
curl -s -X POST -H "$H" -H 'Content-Type: application/json' $U/api/check-ins \
  -d '{"participantId":"d0000000-0000-0000-0000-000000000003","checkpointId":"c0000000-0000-0000-0000-000000000001"}'

# Acknowledge, then resolve, an alert (use an id from the alert list)
curl -s -X POST -H "$H" $U/api/alerts/<alertId>/acknowledge
curl -s -X POST -H "$H" $U/api/alerts/<alertId>/resolve

# OPERATIONS may not create treks: expect 403
curl -s -X POST -H "$H" -H 'Content-Type: application/json' $U/api/treks \
  -d '{"name":"X","startAt":"2027-01-01T00:00:00Z","endAt":"2027-01-02T00:00:00Z"}'
```

To see the delay check work from scratch, log in as `admin` and follow these steps:

1. Create a trek starting a few minutes ago.
2. Add a checkpoint at 0 hours.
3. Set the trek to `ACTIVE`.
4. Register a participant and set them to `ON_TREK`.

Within one scheduler interval, 60 seconds by default, an alert appears and the app log shows an `ALERT ...` notification line. Set `DELAY_CHECK_INTERVAL_MS=5000` in `.env` for faster feedback.

## Known limitations

* No load, performance or contract tests.
* Behavior with more than one application instance is not tested. The scheduler assumes a single instance.
* Notifications go only to the application log. Real delivery is not required by the brief.
* H2 stands in for PostgreSQL in automated tests. Every write path was also checked by hand against PostgreSQL in Docker, but that check is not automated (no Testcontainers).
* No code coverage report is generated.
