package com.interview.app.applicant;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.interview.app.verification.Verification;
import com.interview.app.verification.VerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTOs: the JSON shapes the applicant API accepts and returns.
 * Requests have no {@code id}, {@code currency} or timestamps, so clients can't set them (mass-assignment guard).
 * Validation failures become 422; unparseable values (e.g. {@code "annualIncome": "abc"}) become 400.
 */
public final class ApplicantDtos {

    private ApplicantDtos() { }

    /**
     * Body of {@code POST /api/applicants}. Example:
     * {@code {"name":"Jane Doe","email":"jane@example.com","employer":"Acme","annualIncome":85000.00}}
     */
    public record CreateApplicantRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String employer,
            @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal annualIncome) { }

    /** Body of {@code PUT /api/applicants/{id}}: replaces all editable fields. */
    public record UpdateApplicantRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String employer,
            @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal annualIncome) { }

    /** What the API returns for an applicant. */
    public record ApplicantResponse(Long id, String name, String email, String employer,
                                    BigDecimal annualIncome, String currency,
                                    Instant createdAt, Instant updatedAt) {

        public static ApplicantResponse from(Applicant a) {
            return new ApplicantResponse(a.getId(), a.getName(), a.getEmail(), a.getEmployer(),
                    a.getAnnualIncome(), a.getCurrency(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    /**
     * What {@code GET /api/applicants/{id}} returns: the applicant's fields plus its verifications, oldest first.
     * The open one (if any) is PENDING or NEEDS_REVIEW.
     */
    public record ApplicantDetailResponse(Long id, String name, String email, String employer,
                                          BigDecimal annualIncome, String currency,
                                          Instant createdAt, Instant updatedAt,
                                          List<VerificationSummary> verifications) {

        public static ApplicantDetailResponse from(Applicant a, List<Verification> verifications) {
            return new ApplicantDetailResponse(a.getId(), a.getName(), a.getEmail(), a.getEmployer(),
                    a.getAnnualIncome(), a.getCurrency(), a.getCreatedAt(), a.getUpdatedAt(),
                    verifications.stream().map(VerificationSummary::from).toList());
        }
    }

    /** One verification inside {@link ApplicantDetailResponse}. */
    public record VerificationSummary(Long id, VerificationStatus status, Instant createdAt, Instant updatedAt) {

        public static VerificationSummary from(Verification v) {
            return new VerificationSummary(v.getId(), v.getStatus(), v.getCreatedAt(), v.getUpdatedAt());
        }
    }
}
