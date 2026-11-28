package com.interview.app.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** REPOSITORY for the {@code verifications} table. Spring Data generates the implementation. */
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    /** Roughly: {@code SELECT * FROM verifications WHERE applicant_id = ? ORDER BY id} (uses the applicant_id index). */
    List<Verification> findByApplicant_IdOrderByIdAsc(Long applicantId);
}
