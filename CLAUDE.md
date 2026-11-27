# Interview scaffold: conventions for Claude

Spring Boot 3.5, Java 17, Maven, Spring Data JPA, H2 (file-based in dev, in-memory in tests).
This is a live interview. The candidate drives design; you write focused code they will review.

## How to work
- Make the smallest change that completes the step asked for. Do not add features, layers or files beyond the ask.
- Touch at most a few files per step and list every file you changed at the end.
- Do not run long-lived commands (`mvn spring-boot:run`); the candidate runs the app.
- After code changes, run `mvn -q test` and report the result.
- Never commit unless asked.

## Architecture (MVC, package per layer; copy the Item files as the template)
- Packages under `com.interview.app`: `model` (entities, status enums), `dto`, `repository`, `service`, `controller`, `exception`, `filter`. A new entity gets one file in each layer folder; no per-feature packages.
- controller -> service -> repository. Controllers never call repositories.
- Request/response DTOs are Java records in `<Entity>Dtos.java`. Never bind JPA entities from JSON.
- Business rules and `@Transactional` boundaries live in the service.
- Status enums hold their allowed transitions (see `ItemStatus.canTransitionTo`).
- Enforce business uniqueness with DB unique constraints, not check-then-insert; use `saveAndFlush`.
- Foreign keys: `@ManyToOne(fetch = LAZY, optional = false)`; choose `@OnDelete` deliberately.
- Money: `long` cents or `BigDecimal`, never `double`.

## Errors (ApiExceptionHandler)
- Throw `NotFoundException` -> 404, `ConflictException` -> 409.
- Bean Validation failures -> 422 with field details. Malformed JSON, bad enum value or non-numeric id -> 400. DB constraint violations -> 409.
- Error body: `{"error", "message", "details"?}`.

## Tests
- MockMvc integration tests in `src/test`, one test class per controller, following `ItemControllerTest`.
- For every endpoint cover: happy path, field validation (422), malformed input (400), unknown id (404), duplicate or illegal state (409).

## Security
- Parameterized queries only (Spring Data / JPQL with parameters). No string-built SQL.
- Frontend uses `textContent`, never `innerHTML`, for user data.
- Never log PII (SSN, account numbers); mask in responses.
