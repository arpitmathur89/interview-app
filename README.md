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

## API
| Method | Path | Purpose | Codes |
| --- | --- | --- | --- |
| POST | /api/items | create | 201, 400, 409 |
| GET | /api/items?status=&page=&size= | list, filter, paginate (size max 100) | 200 |
| GET | /api/items/{id} | get one | 200, 400, 404 |
| PUT | /api/items/{id} | update name/description | 200, 400, 404, 409 |
| PATCH | /api/items/{id}/status | status change with transition rules | 200, 400, 404, 409 |
| GET | /api/items/{id}/events | status history (audit) | 200, 404 |
| DELETE | /api/items/{id} | delete (history cascades) | 204, 404 |

Errors always look like `{"error": "...", "message": "...", "details": {...}}`.
Saved requests for every case: `requests.http` (IntelliJ HTTP client).

## Manual API checks
```bash
curl -X POST localhost:8080/api/items -H 'Content-Type: application/json' -d '{"name":"a","description":"b"}'
curl localhost:8080/api/items
curl localhost:8080/api/items/1
curl -X PUT localhost:8080/api/items/1 -H 'Content-Type: application/json' -d '{"name":"a2"}'
curl -X PATCH localhost:8080/api/items/1/status -H 'Content-Type: application/json' -d '{"status":"IN_REVIEW","reason":"docs in"}'
curl localhost:8080/api/items/1/events
curl -X DELETE localhost:8080/api/items/1
```

## Inspect / reset the database
- Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/devdb;AUTO_SERVER=TRUE`, user `sa`, empty password
- Reset: stop the app, then `./reset-db.sh`
- Useful SQL: `SELECT * FROM ITEMS; SELECT * FROM ITEM_EVENTS;`
- Schema is auto-generated from entities (`ddl-auto=update`).

## Logs / errors
- Console output of `mvn spring-boot:run` (SQL is logged via `show-sql`)
- Test reports: `target/surefire-reports/`

## Layout
```
src/main/java/com/interview/app/
  InterviewApplication.java      entry point
  ApiExceptionHandler.java       one JSON error shape: 400 / 404 / 409 / 500
  common/NotFoundException.java  -> 404
  common/ConflictException.java  -> 409
  item/Item.java                 placeholder entity, unique name, status, timestamps
  item/ItemStatus.java           enum + allowed transitions
  item/ItemEvent.java            audit child table, FK to items, ON DELETE CASCADE
  item/ItemDtos.java             request/response records (no mass assignment)
  item/ItemService.java          business rules + @Transactional boundaries
  item/ItemRepository.java, ItemEventRepository.java
  item/ItemController.java       thin REST layer
src/main/resources/
  application.properties
  static/index.html              minimal frontend
src/test/java/.../ItemControllerTest.java
```

## Interview day workflow
1. Read the full prompt, confirm P0/P1, and ask about backend-only scope.
2. Copy the `item` package for each real entity (rename Item -> Applicant, ItemEvent -> VerificationTask, etc.);
   keep controller -> service -> repository and the DTO records.
3. Build one vertical slice (endpoint -> DB -> response), test it, then repeat.
4. Test happy path + missing input, bad IDs, duplicates, updates.

## Security talking points
- SQL injection: JPA/Spring Data use parameterized queries; avoid string-concatenated native queries.
- XSS: frontend uses `textContent`; never `innerHTML` with user data.
- CSRF: not enabled (no Spring Security, no cookie sessions). If auth with cookies is added, enable CSRF protection.
- AuthN/AuthZ: none yet. Add Spring Security only if the interviewer prioritizes it.
- Validation: Bean Validation (`@Valid`) on request DTO records; entities are never bound from JSON, so clients can't set id/status/timestamps (mass assignment).
- Integrity: unique + FK + NOT NULL constraints in the DB, so concurrent duplicates still get a 409.
- Remember to disable the H2 console outside local dev.
