package com.interview.app.verification;

import com.interview.app.verification.VerificationDtos.VerificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * CONTROLLER: HTTP in/out only; delegates to {@link VerificationService}.
 * <pre>
 *   POST /api/applicants/{applicantId}/verifications   start (PENDING)   201 | 400 | 404 | 409
 * </pre>
 * (400 = non-numeric id; 404 = unknown applicant; 409 = applicant already has an open verification.)
 */
@RestController
@RequestMapping("/api/applicants/{applicantId}/verifications")
public class VerificationController {

    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    /** No request body: the applicant comes from the path and the status is always PENDING. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationResponse start(@PathVariable Long applicantId) {
        return service.start(applicantId);
    }
}
