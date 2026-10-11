# Issues and resolutions

Log of problems hit while building the project, with cause and fix. Newest entries go at the end. Add an entry whenever a problem costs real time or changes a decision.

Each entry has: step, symptom, cause, resolution, status.

## 1. Local checkout behind the remote

- **Step:** validation before 0.3.
- **Symptom:** the validation checklist expected local `main` at 31ef46d, one commit ahead of `origin/main`. Local `main` was at 672dc93. The Gradle files and `docs/STEPS.md` were missing.
- **Cause:** the remote `main` already held 776d6d3 (Gradle skeleton) and 31ef46d (steps list). The local clone had never pulled them. `git ls-remote origin` showed the real remote head.
- **Resolution:** `git fetch origin`, then `git merge-base --is-ancestor main origin/main` to confirm a fast-forward was possible, then `git pull --ff-only`. Local `main` then matched `origin/main` at 31ef46d.
- **Status:** resolved.

## 2. Gradle wrapper cannot download the distribution

- **Step:** validation before 0.3.
- **Symptom:** `./gradlew --version` failed with `Downloading from https://services.gradle.org/distributions/gradle-8.14.3-bin.zip failed: timeout (10000ms)` and `SocketTimeoutException: Connect timed out`. A zero-byte `.part` file and a `.lck` file stayed in `~/.gradle/wrapper/dists/gradle-8.14.3-bin/`.
- **Cause:** the JVM could not connect to the host the Gradle download redirects to on this network. `curl` reached the same URL with HTTP 200. Forcing IPv4 with `-Djava.net.preferIPv4Stack=true` did not help. The wrapper timeout is 10 seconds (`networkTimeout=10000`).
- **Resolution:** downloaded the same zip with `curl`, checked it against the published SHA-256 file, deleted the stale `.part` and `.lck` files, and copied the zip into the wrapper's cache directory. The wrapper then unpacked it and `./gradlew --version` reported Gradle 8.14.3 on JVM 21. No repo file changed.
- **Status:** resolved on this machine. A different Gradle version on the same network will fail the same way. Repeat the zip-in-cache step for it.

## 3. Comment claimed a startup failure that does not happen

- **Step:** 0.3.
- **Symptom:** the first draft of `application.yml` said the app refuses to start without `JWT_SECRET`. Starting the default profile without it did not report a missing secret. It failed later on the unreachable database host `db`.
- **Cause:** Spring resolves a `${...}` placeholder only when a bean reads the property. No code reads `app.security.jwt-secret` yet, because `JwtService` comes in step 2.1.
- **Resolution:** corrected the comment to say the secret is resolved when `JwtService` first reads it. The secret still has no default in the default profile, so no weak secret ships.
- **Status:** resolved in step 2.1. `JwtProperties` binds and validates the setting at startup. Without `JWT_SECRET` the app stops with `Binding to target ...JwtProperties failed` for `app.security.jwtSecret`, and a secret shorter than 32 characters stops it the same way. A valid secret starts the app.

## 4. Cannot connect to the database from the host

- **Step:** after 1.4.
- **Symptom:** a database client on the host could not log in to the PostgreSQL container.
- **Cause:** three separate things. The compose file did not publish port 5432 at first. The `DB_PASSWORD` in `.env` was a random value that nobody had written down. The port is bound to `127.0.0.1` only, so a client that resolves `localhost` to the IPv6 address `::1` finds the port closed.
- **Resolution:** published the port on `127.0.0.1:5432` in `docker-compose.yml`. Set a fixed `DB_PASSWORD` in the git-ignored `.env`. PostgreSQL reads `POSTGRES_PASSWORD` only when the data volume is first created, so the existing role was changed with `ALTER USER` instead of recreating the volume. Clients should use host `127.0.0.1`, not `localhost`.
- **Status:** resolved. Login with the new password works from the host, a wrong password is rejected, and the app still starts and logs users in.

## 5. Hibernate could not find tables on H2 in the test profile

- **Step:** 2.4.
- **Symptom:** the first test that started the app on H2 failed with `Schema-validation: missing table [alerts]`, although Flyway had applied both migrations.
- **Cause:** the `test` profile URL carried `DATABASE_TO_UPPER=false`, which I had added in step 0.3 and never ran. With it, Flyway created lowercase table names, while Hibernate looks tables up in the upper case that H2 normally uses.
- **Resolution:** removed `DATABASE_TO_UPPER=false` from the H2 URL in `application.yml`. Flyway and Hibernate then agree on names, and `AuditingRepositoryTest` runs the real V1 and V2 migrations on H2.
- **Status:** resolved. Lesson: a setting is not verified until something runs it. The `test` profile was untested until this step.

## Notes (expected behaviour, not defects)

- **Default profile outside Compose:** it points at host `db`, which only exists inside Docker Compose. Running it on the host fails with a connection error. Use the `local` profile for IDE runs.
- **`test` profile:** not exercised yet. It first runs with the tests in phase 5.
