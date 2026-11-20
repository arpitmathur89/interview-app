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

/** Full request -> controller -> service -> H2 (in-memory) -> response tests. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ItemRepository items;
    @Autowired ItemEventRepository events;

    @BeforeEach
    void clean() {
        events.deleteAllInBatch();
        items.deleteAllInBatch();
    }

    // ---------- happy paths ----------

    @Test
    void createThenRetrieve() throws Exception {
        long id = create("first");

        mvc.perform(get("/api/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("first"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists());

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updatePersists() throws Exception {
        long id = create("old");
        mvc.perform(put("/api/items/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new\",\"description\":\"d\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new"));
        mvc.perform(get("/api/items/{id}", id)).andExpect(jsonPath("$.description").value("d"));
    }

    @Test
    void validTransitionIsRecordedInHistory() throws Exception {
        long id = create("t");
        patchStatus(id, "IN_REVIEW").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_REVIEW"));
        patchStatus(id, "APPROVED").andExpect(status().isOk());

        mvc.perform(get("/api/items/{id}/events", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].toStatus").value("APPROVED"));
    }

    @Test
    void filterByStatusAndPaginate() throws Exception {
        long a = create("a");
        create("b");
        create("c");
        patchStatus(a, "IN_REVIEW");

        mvc.perform(get("/api/items").param("status", "IN_REVIEW"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("a"));
        mvc.perform(get("/api/items").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("c"));
    }

    @Test
    void deleteRemovesItemAndHistory() throws Exception {
        long id = create("gone");
        patchStatus(id, "IN_REVIEW");
        mvc.perform(delete("/api/items/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/items/{id}", id)).andExpect(status().isNotFound());
    }

    // ---------- bad input ----------

    @Test
    void blankNameIsRejectedWithFieldError() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void malformedJsonAndBadEnumAre400() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
        long id = create("x");
        patchStatus(id, "NOPE").andExpect(status().isBadRequest());
    }

    @Test
    void clientCannotSetIdOrStatus() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":424242,\"name\":\"sneaky\",\"status\":\"APPROVED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(424242)))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ---------- invalid identifiers ----------

    @Test
    void unknownIdIs404Everywhere() throws Exception {
        mvc.perform(get("/api/items/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
        mvc.perform(put("/api/items/999999").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"n\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/items/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/items/999999/events")).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdIs400() throws Exception {
        mvc.perform(get("/api/items/abc")).andExpect(status().isBadRequest());
    }

    // ---------- duplicates and state conflicts ----------

    @Test
    void duplicateNameIs409() throws Exception {
        create("dup");
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"dup\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void illegalTransitionIs409() throws Exception {
        long id = create("t");
        patchStatus(id, "APPROVED").andExpect(status().isConflict()); // PENDING -> APPROVED not allowed
    }

    @Test
    void repeatingSameStatusIsIdempotent() throws Exception {
        long id = create("t");
        patchStatus(id, "IN_REVIEW").andExpect(status().isOk());
        patchStatus(id, "IN_REVIEW").andExpect(status().isOk());
        mvc.perform(get("/api/items/{id}/events", id)).andExpect(jsonPath("$", hasSize(1)));
    }

    // ---------- helpers ----------

    private long create(String name) throws Exception {
        String json = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions patchStatus(long id, String status) throws Exception {
        return mvc.perform(patch("/api/items/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\",\"reason\":\"test\"}"));
    }
}
