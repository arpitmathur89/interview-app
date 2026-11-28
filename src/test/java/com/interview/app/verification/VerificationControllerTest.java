package com.interview.app.verification;

import com.interview.app.applicant.Applicant;
import com.interview.app.applicant.ApplicantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc integration tests for {@code POST /api/applicants/{applicantId}/verifications}. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class VerificationControllerTest {

    private static final String START = "/api/applicants/{applicantId}/verifications";

    @Autowired MockMvc mvc;
    @Autowired ApplicantRepository applicants;
    @Autowired VerificationRepository verifications;
    @Autowired TransactionTemplate tx;

    long applicantId;

    @BeforeEach
    void setUp() {
        verifications.deleteAllInBatch();
        applicants.deleteAllInBatch();
        applicantId = applicants.saveAndFlush(
                new Applicant("Jane", "jane@example.com", "Acme", new BigDecimal("1000"))).getId();
    }

    /** 201 with a PENDING verification for the applicant. */
    @Test
    void startCreatesPendingVerification() throws Exception {
        mvc.perform(post(START, applicantId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.applicantId").value(applicantId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.pendingApplicantId").doesNotExist());
    }

    /** 409 when the applicant already has a PENDING verification. */
    @Test
    void secondStartWhilePendingIs409() throws Exception {
        mvc.perform(post(START, applicantId)).andExpect(status().isCreated());
        mvc.perform(post(START, applicantId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    /** Once the previous verification is final, a new one can be started. */
    @Test
    void canStartAgainAfterPreviousIsFinal() throws Exception {
        mvc.perform(post(START, applicantId)).andExpect(status().isCreated());
        markFinal(verifications.findAll().get(0).getId());

        mvc.perform(post(START, applicantId)).andExpect(status().isCreated());
    }

    /** GET /api/applicants/{id} includes the verifications with their current status, oldest first. */
    @Test
    void getApplicantShowsVerificationStatus() throws Exception {
        mvc.perform(post(START, applicantId)).andExpect(status().isCreated());
        markFinal(verifications.findAll().get(0).getId());
        mvc.perform(post(START, applicantId)).andExpect(status().isCreated());

        mvc.perform(get("/api/applicants/{id}", applicantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane"))
                .andExpect(jsonPath("$.verifications", hasSize(2)))
                .andExpect(jsonPath("$.verifications[0].status").value("VERIFIED"))
                .andExpect(jsonPath("$.verifications[1].status").value("PENDING"))
                .andExpect(jsonPath("$.verifications[0].pendingApplicantId").doesNotExist());
    }

    /** 404 for an applicant that doesn't exist. */
    @Test
    void unknownApplicantIs404() throws Exception {
        mvc.perform(post(START, 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    /** 400 for a non-numeric applicant id. */
    @Test
    void nonNumericApplicantIdIs400() throws Exception {
        mvc.perform(post("/api/applicants/abc/verifications")).andExpect(status().isBadRequest());
    }

    /** Moves a verification to VERIFIED inside a transaction, so its lazy applicant can be read. */
    private void markFinal(long verificationId) {
        tx.executeWithoutResult(s -> {
            Verification v = verifications.findById(verificationId).orElseThrow();
            v.setStatus(VerificationStatus.VERIFIED);
        });
    }
}
