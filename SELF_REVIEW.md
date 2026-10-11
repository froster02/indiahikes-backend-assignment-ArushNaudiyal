# Self review

## Strengths

* **The core rule is correct and safe to repeat.** The delay check scans only `ACTIVE` treks that have started and `ON_TREK` participants. It reads time from an injected `Clock`, and it raises at most one alert per participant and checkpoint. It is tested with a fixed clock at exact moments and against the seed data on H2. In Docker, the first run raised 12 alerts and later runs raised none.
* **Data integrity sits in the database as well as the code.** Foreign keys, unique constraints and `CHECK` constraints back the service rules. A race on a duplicate check-in is stopped by the unique constraint and still returns a clean 409.
* **The API behaves predictably.**
  * One `ApiError` shape is used for every failure, including 401 and 403 from the security layer.
  * Validation errors name each field.
  * State changes that are not allowed return 422 with a clear message.
  * Unknown sort fields return 400 instead of a 500.
* **Security is stateless and simple.**
  * Tokens are JWT, and passwords are BCrypt hashes.
  * The role rules sit in one class.
  * A missing or short `JWT_SECRET` stops the app at startup.
  * Login does not reveal which usernames exist.
* **Behavior is configurable without code changes.** The scheduler interval, on and off switch, grace period, token lifetime and notification channels all come from the environment, and each setting is validated at startup.
* **Notifications are easy to extend.** A new channel is one class plus its name in `NOTIFICATION_CHANNELS`. A failing channel does not stop the others.
* **Tests and documentation.** There are 155 automated tests across services, controllers, H2 SQL and the full application. Every feature was also run by hand against PostgreSQL in Docker before its commit. `docs/ISSUES.md` records each real problem found during the build and how it was fixed.

## Weaknesses

* **There is no optimistic locking on alerts.** Two users acknowledging or resolving the same alert at the same moment both succeed, and the later write wins. Adding `@Version` to `Alert` would turn the second request into a conflict.
* **The delay check is chatty with the database.** It makes two existence queries per participant and checkpoint. That is fine for a few treks, but slow for thousands. One query per trek that returns the existing check-ins and alerts would scale better.
* **Services return web DTOs.** This keeps entities out of controllers, but ties the service layer to the API shape.
* **The search uses `LIKE '%x%'`.** Partial name and phone search cannot use an index.
* **Login has no rate limit or lockout.** BCrypt slows guessing, but nothing stops many attempts.

## Known limitations

* **Single node only.** Several app instances would each run the scheduler. Alerts could then be raised twice in a race, because the duplicate guard is a check followed by an insert, without a database constraint on active alerts.
* **Tokens cannot be revoked.** A token stays valid until it expires, 8 hours by default, even after the user is removed or changes role. There is no logout or refresh.
* **Notifications are not delivered.** They go to the application log only, run on the scheduler thread, and have no retry.
* **There are no update or delete endpoints** for treks, checkpoints or participants. The brief does not require them, but real use would need corrections.
* **A missed checkpoint keeps its alert.** If a participant reports at a later checkpoint but never at an earlier one, the alert for the earlier one remains until someone resolves it. That is deliberate, but it may be noisy.
* **H2 stands in for PostgreSQL in automated tests.** The manual checks used PostgreSQL, but they are not automated.

## What I would improve next

1. Add `@Version` to `Alert` and return 409 on a lost update.
2. Make the delay check use one set-based query per trek. Add ShedLock, or a partial unique index on active alerts, so several instances are safe.
3. Run the repository and integration tests against PostgreSQL with Testcontainers.
4. Add refresh tokens with short-lived access tokens, and rate-limit login.
5. Send notifications asynchronously with retries, and add a real channel such as email or a webhook.
6. Add update and deactivate endpoints for treks, checkpoints and participants, with the same audit trail.
7. Add a code coverage report and Spring Boot Actuator for metrics.

## AI assistance

This project was built with Claude Code, an AI coding assistant, working step by step from `docs/STEPS.md`. The owner directed the work and set the scope. Phases 0 to 2 and steps 3.1 to 3.4 were approved commit by commit. Steps 3.5 to 5.3 were delegated in one instruction and committed by the assistant under the same rules.

The working rules in `CLAUDE.md` governed every step:

* Check each step against the PRD before writing code.
* After each change, run the full build with all tests, rebuild the Docker stack and exercise the endpoints live.
* Commit locally only after review, and push only with explicit approval.
* Log real problems in `docs/ISSUES.md`.

That log shows six problems found by those checks rather than assumed away, including a test profile that had never run and a health check that hung while the database was down.

Parts of the AI-written code that most deserve careful human review:

* **`SecurityConfig` and `JwtAuthFilter`.** The order of the role rules, and which paths are public.
* **`DelayDetectionService`.** The boundaries of "late", the grace period, and the decision to never raise an alert twice for the same pair.
* **`CheckInService`.** The order of the validations and the handling of a race on the unique constraint.
* **The transaction boundaries.** Specifically, the notification listener running after commit.
