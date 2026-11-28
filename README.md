# Interview App Scaffold

Spring Boot 3.5 / Java 17 / Maven / Spring Data JPA / H2 (file-based) / static HTML frontend.

## Prerequisites
- JDK 17+ (`java -version`)
- Maven 3.9+ (`mvn -v`)  -- or use your IDE's Maven integration

## Run
```bash
mvn spring-boot:run
```
App: http://localhost:8080  (form + list UI)

## Test
```bash
mvn test                 # unit/integration tests (in-memory DB)
./smoke-test.sh          # curl checks against the running app
```

## Make shortcuts
`make reset` | `make run` | `make seed` | `make smoke` | `make test` (each request logs method, path, status and ms)

## API
| Method | Path | Purpose | Codes |
| --- | --- | --- | --- |
| POST | /api/items | create | 201, 400, 409, 422 |
| GET | /api/items | list all | 200 |
| GET | /api/items/{id} | get one | 200, 400, 404 |
| PUT | /api/items/{id} | update name/description | 200, 400, 404, 409, 422 |
| DELETE | /api/items/{id} | delete | 204, 400, 404 |

Errors always look like `{"error": "...", "message": "...", "details": {...}}`.
Saved requests for every case: `requests.http` (IntelliJ HTTP client).

## Manual API checks
```bash
curl -X POST localhost:8080/api/items -H 'Content-Type: application/json' -d '{"name":"a","description":"b"}'
curl localhost:8080/api/items
curl localhost:8080/api/items/1
curl -X PUT localhost:8080/api/items/1 -H 'Content-Type: application/json' -d '{"name":"a2"}'
curl -X DELETE localhost:8080/api/items/1
```

## Inspect / reset the database
- Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:~/.interview-app/devdb;AUTO_SERVER=TRUE`, user `sa`, empty password
- Reset: stop the app, then `./reset-db.sh`
- Useful SQL: `SELECT * FROM ITEMS;`
- Schema is auto-generated from entities (`ddl-auto=update`).

## Logs / errors
- Console output of `mvn spring-boot:run` (SQL is logged via `show-sql`)
- Test reports: `target/surefire-reports/`

## Layout (one folder per feature; `applicant` is the template)
A request flows: browser -> controller -> service -> repository -> entity (DB) and back as JSON.
Each feature package holds its entity (+ status enum), DTO records, repository, service and controller.
```
src/main/java/com/interview/app/
  InterviewApplication.java          entry point
  applicant/    template feature: CRUD at /api/applicants (GET by id includes verifications)
    Applicant.java                   entity: name, email, employer, annualIncome (NUMERIC 19,2), currency=USD
    ApplicantDtos.java               request/response records (no mass assignment)
    ApplicantRepository.java         DB access (Spring Data writes the SQL)
    ApplicantService.java            business rules + @Transactional boundaries
    ApplicantController.java         HTTP in/out only, thin
  verification/ POST /api/applicants/{applicantId}/verifications
    Verification.java                FK to applicants (ON DELETE CASCADE), unique nullable pendingApplicantId
    VerificationStatus.java          PENDING | VERIFIED | NEEDS_REVIEW | FAILED + allowed transitions
    VerificationDtos.java, VerificationRepository.java, VerificationService.java, VerificationController.java
  item/         original placeholder feature: CRUD at /api/items
    Item.java, ItemDtos.java, ItemRepository.java, ItemService.java, ItemController.java
  exception/    shared: errors -> one JSON shape: 400 / 404 / 409 / 422 / 500
    ApiExceptionHandler.java, NotFoundException.java (404), ConflictException.java (409)
  filter/       shared: one log line per request
    RequestLoggingFilter.java
src/main/resources/
  application.properties
  static/index.html                  the page; calls the JSON API with fetch()
src/test/java/com/interview/app/     tests mirror the feature packages
  applicant/ApplicantControllerTest.java
  verification/VerificationControllerTest.java
  item/ItemControllerTest.java
```

## Interview day workflow
1. Read the full prompt, confirm P0/P1, and ask about backend-only scope.
2. For each new entity, create a feature package by copying the `applicant` package (entity, DTOs,
   repository, service, controller) plus its test package.
3. Build one vertical slice (endpoint -> DB -> response), test it, then repeat.
4. Test happy path + missing input, bad IDs, duplicates, updates.


