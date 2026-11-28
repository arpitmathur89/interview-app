package com.interview.app.applicant;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc integration tests for {@code /api/applicants}, against an in-memory H2 database. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ApplicantControllerTest {

    private static final String BASE = "/api/applicants";

    @Autowired MockMvc mvc;
    @Autowired ApplicantRepository applicants;

    @BeforeEach
    void clean() {
        applicants.deleteAllInBatch();
    }

    // ---------- happy paths ----------

    /** Create, then read back by id and in the list; currency defaults to USD. */
    @Test
    void createThenRetrieve() throws Exception {
        long id = create(body("Jane Doe", "jane@example.com", "Acme", "85000.50"));

        mvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.employer").value("Acme"))
                .andExpect(jsonPath("$.annualIncome").value(85000.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.verifications", hasSize(0)));

        mvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    /** PUT changes the applicant, and a fresh GET proves it was saved. */
    @Test
    void updatePersists() throws Exception {
        long id = create(body("Old", "old@example.com", "Initech", "1000"));
        mvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("New", "new@example.com", "Globex", "2000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
        mvc.perform(get(BASE + "/{id}", id))
                .andExpect(jsonPath("$.employer").value("Globex"))
                .andExpect(jsonPath("$.annualIncome").value(2000));
    }

    @Test
    void deleteRemovesApplicant() throws Exception {
        long id = create(body("Gone", "gone@example.com", "Acme", "1"));
        mvc.perform(delete(BASE + "/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/{id}", id)).andExpect(status().isNotFound());
    }

    // ---------- field validation (422) ----------

    @Test
    void invalidFieldsAre422() throws Exception {
        post("{\"name\":\"\",\"email\":\"not-an-email\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.employer").exists())
                .andExpect(jsonPath("$.details.annualIncome").exists());
    }

    /** Negative income and more than 2 decimal places are rejected. */
    @Test
    void badIncomeIs422() throws Exception {
        post(body("A", "a@example.com", "Acme", "-1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.annualIncome").exists());
        post(body("A", "a@example.com", "Acme", "1.234"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- malformed input (400) ----------

    @Test
    void malformedInputIs400() throws Exception {
        post("{not json").andExpect(status().isBadRequest());
        post("{\"name\":\"A\",\"email\":\"a@example.com\",\"employer\":\"Acme\",\"annualIncome\":\"abc\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/abc")).andExpect(status().isBadRequest());
    }

    /** Client-sent id and currency are ignored. */
    @Test
    void clientCannotSetIdOrCurrency() throws Exception {
        post("{\"id\":424242,\"name\":\"A\",\"email\":\"a@example.com\",\"employer\":\"Acme\","
                + "\"annualIncome\":10,\"currency\":\"EUR\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(424242)))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    // ---------- unknown id (404) ----------

    @Test
    void unknownIdIs404Everywhere() throws Exception {
        mvc.perform(get(BASE + "/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
        mvc.perform(put(BASE + "/999999").contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "a@example.com", "Acme", "1")))
                .andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private static String body(String name, String email, String employer, String income) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"employer\":\"" + employer
                + "\",\"annualIncome\":" + income + "}";
    }

    private ResultActions post(String json) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BASE)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long create(String json) throws Exception {
        String res = post(json)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }
}
