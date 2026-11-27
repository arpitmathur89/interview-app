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
  - JDBC URL: `jdbc:h2:file:./data/devdb;AUTO_SERVER=TRUE`, user `sa`, empty password
- Reset: stop the app, then `./reset-db.sh`
- Useful SQL: `SELECT * FROM ITEMS;`
- Schema is auto-generated from entities (`ddl-auto=update`).

## Logs / errors
- Console output of `mvn spring-boot:run` (SQL is logged via `show-sql`)
- Test reports: `target/surefire-reports/`

## Layout (MVC, one folder per layer)
A request flows: View (browser) -> controller -> service -> repository -> model (DB) and back as JSON.
```
src/main/java/com/interview/app/
  InterviewApplication.java          entry point
  model/        M: what is stored (JPA entities = DB tables)
    Item.java                        placeholder entity, unique name, timestamps
  dto/          API contract: request/response records (no mass assignment)
    ItemDtos.java
  controller/   C: HTTP in/out only, thin
    ItemController.java
  service/      business rules + @Transactional boundaries
    ItemService.java
  repository/   DB access (Spring Data writes the SQL)
    ItemRepository.java
  exception/    errors -> one JSON shape: 400 / 404 / 409 / 422 / 500
    ApiExceptionHandler.java, NotFoundException.java (404), ConflictException.java (409)
  filter/       cross-cutting: one log line per request
    RequestLoggingFilter.java
src/main/resources/
  application.properties
  static/index.html                  V: the page; calls the JSON API with fetch()
src/test/java/com/interview/app/controller/ItemControllerTest.java
```

## Interview day workflow
1. Read the full prompt, confirm P0/P1, and ask about backend-only scope.
2. For each real entity (Item -> Applicant, etc.), copy the Item file in each layer
   folder: model, dto, repository, service, controller, plus its test.
3. Build one vertical slice (endpoint -> DB -> response), test it, then repeat.
4. Test happy path + missing input, bad IDs, duplicates, updates.
