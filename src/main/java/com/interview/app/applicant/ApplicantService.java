package com.interview.app.applicant;

import com.interview.app.applicant.ApplicantDtos.*;
import com.interview.app.exception.NotFoundException;
import com.interview.app.verification.VerificationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICE: business rules and transaction boundaries for applicants.
 * Takes and returns DTOs; entities never leave this class.
 */
@Service
public class ApplicantService {

    private final ApplicantRepository applicants;
    private final VerificationRepository verifications;

    public ApplicantService(ApplicantRepository applicants, VerificationRepository verifications) {
        this.applicants = applicants;
        this.verifications = verifications;
    }

    /** Every applicant, ordered by id. */
    @Transactional(readOnly = true)
    public List<ApplicantResponse> list() {
        return applicants.findAll(Sort.by("id")).stream().map(ApplicantResponse::from).toList();
    }

    /**
     * One applicant with its verifications; 404 if the id doesn't exist.
     * Both queries and the DTO mapping run inside this one read-only transaction, so the response is a consistent
     * snapshot and no lazy entity is touched after the transaction closes.
     */
    @Transactional(readOnly = true)
    public ApplicantDetailResponse get(Long id) {
        Applicant applicant = find(id);
        return ApplicantDetailResponse.from(applicant, verifications.findByApplicant_IdOrderByIdAsc(id));
    }

    /** Creates an applicant. Currency is defaulted to USD by the entity. */
    @Transactional
    public ApplicantResponse create(CreateApplicantRequest req) {
        Applicant saved = applicants.saveAndFlush(new Applicant(
                req.name().trim(), req.email().trim(), req.employer().trim(), req.annualIncome()));
        return ApplicantResponse.from(saved);
    }

    /** Replaces the editable fields (PUT semantics); 404 if missing. */
    @Transactional
    public ApplicantResponse update(Long id, UpdateApplicantRequest req) {
        Applicant a = find(id);
        a.setName(req.name().trim());
        a.setEmail(req.email().trim());
        a.setEmployer(req.employer().trim());
        a.setAnnualIncome(req.annualIncome());
        return ApplicantResponse.from(applicants.saveAndFlush(a));
    }

    /** Deletes an applicant; 404 if missing. */
    @Transactional
    public void delete(Long id) {
        applicants.delete(find(id));
    }

    private Applicant find(Long id) {
        return applicants.findById(id).orElseThrow(() -> new NotFoundException("Applicant " + id + " not found"));
    }
}
