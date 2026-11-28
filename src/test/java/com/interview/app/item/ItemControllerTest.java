package com.interview.app.item;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the whole stack: request -> filter -> controller -> service -> repository -> H2 -> response.
 * Run with {@code make test} or {@code mvn test}.
 *
 * <ul>
 *   <li>{@code @SpringBootTest} starts the real application (all beans) inside the test. The {@code properties}
 *       override points it at an <b>in-memory</b> H2 database ({@code jdbc:h2:mem:testdb}), so tests never touch your
 *       dev data in {@code ./data}. {@code DB_CLOSE_DELAY=-1} keeps that database alive until the tests finish.</li>
 *   <li>{@code @AutoConfigureMockMvc} provides {@link MockMvc}: it sends fake HTTP requests straight into Spring MVC
 *       without opening a network port, so the tests are fast but still exercise JSON parsing, validation,
 *       error handling and real SQL.</li>
 *   <li>{@code @Autowired} asks Spring to inject a bean into a field (fine in tests; app code uses constructors).</li>
 * </ul>
 *
 * <p>Reading a test: {@code mvc.perform(request)} sends a request; each {@code .andExpect(...)} is an assertion.
 * {@code jsonPath("$.name")} picks a field out of the JSON response ({@code $} is the root, {@code $[0]} the first
 * array element). If any expectation fails, the test fails and prints the actual response.
 *
 * <p>Coverage follows the interview checklist: happy path, invalid input (400/422), unknown id (404),
 * duplicate or illegal state (409).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ItemRepository items;

    /**
     * Runs before <b>each</b> {@code @Test}: empties the table so every test starts from a clean database and tests
     * can't affect each other.
     */
    @BeforeEach
    void clean() {
        items.deleteAllInBatch();
    }

    // ---------- happy paths ----------

    /** Create, then read it back by id and in the list; timestamps are set by the server. */
    @Test
    void createThenRetrieve() throws Exception {
        long id = create("first");

        mvc.perform(get("/api/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("first"))
                .andExpect(jsonPath("$.createdAt").exists());

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /** PUT changes the item, and a fresh GET proves the change was saved to the DB (not just echoed back). */
    @Test
    void updatePersists() throws Exception {
        long id = create("old");
        mvc.perform(put("/api/items/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new\",\"description\":\"d\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new"));
        mvc.perform(get("/api/items/{id}", id)).andExpect(jsonPath("$.description").value("d"));
    }

    /** DELETE returns 204, and the item is gone afterwards. */
    @Test
    void deleteRemovesItem() throws Exception {
        long id = create("gone");
        mvc.perform(delete("/api/items/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/items/{id}", id)).andExpect(status().isNotFound());
    }

    // ---------- bad input ----------

    /** A blank name breaks {@code @NotBlank}: 422 and {@code details.name} explains why. */
    @Test
    void blankNameIsRejectedWithFieldError() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.name").exists());
    }

    /** Unparseable JSON is 400 (not 500), on both create and update. */
    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
        long id = create("x");
        mvc.perform(put("/api/items/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    /** Mass-assignment guard: an extra {@code id} key is ignored because the DTO has no such field. */
    @Test
    void clientCannotSetId() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":424242,\"name\":\"sneaky\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(424242)));
    }

    // ---------- invalid identifiers ----------

    /** Every endpoint that takes an id answers 404 with the standard error body when the id doesn't exist. */
    @Test
    void unknownIdIs404Everywhere() throws Exception {
        mvc.perform(get("/api/items/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
        mvc.perform(put("/api/items/999999").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"n\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/items/999999")).andExpect(status().isNotFound());
    }

    /** "abc" can't become a Long id: a clean 400, not a 500 crash. */
    @Test
    void nonNumericIdIs400() throws Exception {
        mvc.perform(get("/api/items/abc")).andExpect(status().isBadRequest());
    }

    // ---------- duplicates ----------

    /** The DB unique constraint on name turns a second "dup" into 409, on create and on rename via PUT. */
    @Test
    void duplicateNameIs409() throws Exception {
        create("dup");
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"dup\"}"))
                .andExpect(status().isConflict());
        long other = create("other");
        mvc.perform(put("/api/items/{id}", other).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"dup\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- helpers ----------

    /**
     * Helper: POSTs a new item, checks it returned 201 with a Location header, and returns the generated id
     * (read from the JSON response with JsonPath) so the test can use it in the next request.
     */
    private long create(String name) throws Exception {
        String json = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
