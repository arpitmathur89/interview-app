package com.interview.app.verification;

import com.interview.app.applicant.Applicant;
import com.interview.app.applicant.ApplicantRepository;
import com.interview.app.exception.NotFoundException;
import com.interview.app.verification.VerificationDtos.VerificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SERVICE: business rules and transaction boundaries for verifications. */
@Service
public class VerificationService {

    private final VerificationRepository verifications;
    private final ApplicantRepository applicants;

    public VerificationService(VerificationRepository verifications, ApplicantRepository applicants) {
        this.verifications = verifications;
        this.applicants = applicants;
    }

    /**
     * Starts a verification for an applicant, with status PENDING and {@code pendingApplicantId} = the applicant id.
     * <ul>
     *   <li>Unknown applicant: {@link NotFoundException} (404).</li>
     *   <li>Applicant already has an open verification: not checked with a SELECT first. The INSERT runs and the
     *       UNIQUE {@code pending_applicant_id} column rejects it; Spring raises
     *       {@code DataIntegrityViolationException}, which {@code ApiExceptionHandler} maps to 409. Two simultaneous
     *       requests can't both get through.</li>
     * </ul>
     */
    @Transactional
    public VerificationResponse start(Long applicantId) {
        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException("Applicant " + applicantId + " not found"));
        Verification verification = new Verification(applicant);
        verification.setPendingApplicantId(applicant.getId());
        Verification saved = verifications.saveAndFlush(verification);
        return VerificationResponse.from(saved);
    }
}
