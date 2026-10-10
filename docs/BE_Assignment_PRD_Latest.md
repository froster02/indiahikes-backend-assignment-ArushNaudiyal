# Trek Operations Platform – Backend Take-home Assignment

## Context

Trek Operations Platform operates multiple treks with participants moving through predefined checkpoints.

The operations team needs visibility into participant progress and the ability to identify delayed participants automatically.

This assignment focuses on backend design and implementation using Spring Boot.

Do not build any UI.

The expected effort is approximately **8–10 hours**.

---

# Objective

This assignment is designed to evaluate your skills in building a Spring Boot backend with:

- REST API design
- Authentication and role-based access
- Background processing and scheduling
- Configuration management
- Error handling
- Testing
- Clean code and maintainable architecture
- Dockerized deployment

We are more interested in your design decisions and trade-offs than in implementing every possible feature.

Prefer readability and maintainability over implementing every possible feature.

---

# Tech Stack

Please use:

- Java 17 or 21
- Spring Boot 3.x
- Gradle
- Docker
- Docker Compose

Database choice is left to your discretion.

---

# Development Instructions

Create a private Github repository named:

```txt
indiahikes-backend-assignment-yourFullName
```

Add the following Github users as collaborator:

```txt
- KartikWatts
- Manasa-NL
```

Include a `README.md` at the root of the repository describing:

- Setup instructions
- How to build the project
- How to run locally
- How to run using Docker Compose
- Authentication details, if applicable
- Test commands
- Seed data information

It is preferred to include initial data with at least one administrator user.

The application should be runnable with a single command using Docker Compose.

---

# Assumptions

You may assume:

- Operations users have network connectivity while using the system.
- Participants communicate their arrival to operations staff outside the system.
- Operations users record participant check-ins through the APIs.
- The mechanism by which participants communicate their arrival is outside the scope of this assignment.

---

# Assignment

Build a backend service that supports:

1. Trek and checkpoint management
2. Participant management
3. Participant check-ins
4. Automatic detection of delayed participants
5. Alert generation and management
6. Secure APIs with role-based access

---

# Business Requirement

Each trek contains a sequence of checkpoints.

Example:

```txt
Kedarkantha Trek

1. Dehradun
2. Sankri
3. Juda Ka Talab
4. Kedarkantha Summit
5. Sankri
6. Dehradun
```

Participants are expected to report at checkpoints during the trek.

Operations users record participant check-ins in the system.

The system should identify participants who fail to report as expected and generate alerts.

---

# What You Need To Implement

## 1. Trek Management

At minimum, capture:

```txt
Name
Start date
End date
Status
```

---

## 2. Participant Management

At minimum, capture:

```txt
Name
Phone number
Emergency contact
Status
```

Participants belong to a trek.

---

## 3. Checkpoint Management

Treks contain checkpoints.

At minimum, capture:

```txt
Name
Sequence
Expected reporting information
```

Example:

```txt
Trek start time:
2026-08-10 06:00

Checkpoint              Expected After

Dehradun                0 hours
Sankri                  8 hours
Juda Ka Talab           28 hours
Kedarkantha Summit      48 hours
Sankri                  72 hours
Dehradun                84 hours
```

Participants are expected to check in at each checkpoint.

The exact interpretation and validation rules are left to your discretion.

---

## 4. Participant Check-In

Operations users should be able to record participant check-ins.

Capture:

```txt
Participant
Checkpoint
Timestamp
Recorded by
```

You may decide any validations or constraints that are required.

---

## 5. Alert Management

Participants who do not report in time should generate alerts.

Capture:

```txt
Participant
Checkpoint
Status
Created time
```

Operations users should be able to view and manage alerts.

---

## 6. Background Processing

Implement periodic processing to detect delayed participants and generate alerts.

Behavior, scheduling approach, and configuration are left to your discretion.

---

## 7. Authentication and Authorization

Secure the APIs.

Support multiple user roles.

Permissions and implementation details are up to your discretion.

---

## 8. Search APIs

Provide APIs to search participants.

Support:

- Filtering
- Pagination
- Sorting

Exact filters are your choice.

---

## 9. Configuration

Support multiple environments.

Some application behavior should be externally configurable.

---

## 10. Error Handling

Implement a consistent API error response structure.

---

## 11. Audit Information

Track basic audit information for entities.

Implementation approach is your choice.

---

## 12. Health Endpoint

Expose a public endpoint that provides basic application information.

---

## 13. Notifications

The system should support notifying operations users when alerts are created.

Actual notification delivery is not required.

Design the system in a way that allows additional notification channels to be introduced in the future.

---

## 14. API Documentation

Provide API documentation using Swagger/OpenAPI.

The APIs should be easy to explore and test.

---

# Testing Requirements

Test coverage is expected for important business flows.

At minimum:

- Service layer tests
- Controller layer tests

Please use:

- JUnit
- Mockito

Explain your testing strategy in the documentation.

---

# What To Submit

Submit your code along with:

```txt
README.md
DOMAIN_MODEL.md
DECISIONS.md
SELF_REVIEW.md
TEST_NOTES.md
```

If AI tools or coding assistants were used, please include the session logs if available.

---

# DOMAIN_MODEL.md

Describe:

```txt
Entities
Relationships
State transitions
Important constraints
```

---

# DECISIONS.md

Document important design decisions and trade-offs.

Examples:

```txt
Security approach
Scheduler approach
Concurrency considerations
Search implementation
Status modeling
Configuration approach
Notification design
```

---

# SELF_REVIEW.md

Review your own implementation.

Include:

```txt
Strengths
Weaknesses
Known limitations
What you would improve next
```

If AI assistance was used, mention which parts you reviewed carefully and where you are less confident.

---

# TEST_NOTES.md

Include:

```txt
Setup steps
Commands to run
How to test APIs
Known limitations
```

---

# What Not To Do

Please avoid:

```txt
Building a UI
Hardcoding behavior
Ignoring security
Ignoring validation
Replacing Spring Boot architecture completely
Submitting code you cannot explain
```

---

# Evaluation Criteria

We will evaluate:

```txt
Domain understanding
Backend design
API design
Security
Scheduler design
Configuration management
Testing
Code organization
Trade-off decisions
Documentation quality
Ability to explain implementation
```

---

# Follow-up Discussion

During the discussion, we may ask you to walk through:

```txt
Your domain model
Important assumptions
API design choices
Concurrency considerations
Scheduler behavior
Security approach
Testing strategy
Trade-offs
What you would change before production
How AI tools were used
```
