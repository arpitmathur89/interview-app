package com.interview.app.verification;

import com.interview.app.applicant.Applicant;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * MODEL: one {@code Verification} = one row in {@code verifications}, a child of {@code applicants}.
 * <pre>
 *   CREATE TABLE verifications (
 *     id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     applicant_id         BIGINT      NOT NULL REFERENCES applicants(id) ON DELETE CASCADE,
 *     status               VARCHAR(20) NOT NULL,     -- PENDING | VERIFIED | NEEDS_REVIEW | FAILED
 *     pending_applicant_id BIGINT      NULL UNIQUE,  -- = applicant_id while open, NULL once final
 *     created_at           TIMESTAMP   NOT NULL,
 *     updated_at           TIMESTAMP   NOT NULL
 *   );
 * </pre>
 *
 * <p><b>One open verification per applicant</b>, enforced by the database: {@code pending_applicant_id} holds the
 * applicant id while the status is PENDING or NEEDS_REVIEW, and NULL once VERIFIED or FAILED. UNIQUE allows many
 * NULLs, so finished verifications never collide, but a second open one for the same applicant is rejected
 * ({@code DataIntegrityViolationException}, mapped to 409 by {@code ApiExceptionHandler}).
 */
@Entity
@Table(name = "verifications",
        indexes = @Index(name = "ix_verifications_applicant_id", columnList = "applicant_id"))
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent applicant (FK {@code applicant_id}). {@code LAZY}: not loaded unless asked. {@code optional = false}:
     * every verification has an applicant. {@code @OnDelete(CASCADE)}: deleting an applicant deletes its
     * verifications, so {@code DELETE /api/applicants/{id}} keeps working.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Applicant applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    /**
     * Unique and nullable: set to the applicant id by {@code VerificationService.start}, cleared to NULL by
     * {@link #setStatus} once the verification is final.
     */
    @Column(name = "pending_applicant_id", unique = true, nullable = true)
    private Long pendingApplicantId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Empty constructor required by JPA. */
    protected Verification() { }

    /** New verifications start as PENDING. The caller sets {@code pendingApplicantId}. */
    public Verification(Applicant applicant) {
        this.applicant = applicant;
        this.status = VerificationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Applicant getApplicant() { return applicant; }
    public VerificationStatus getStatus() { return status; }
    public Long getPendingApplicantId() { return pendingApplicantId; }
    public void setPendingApplicantId(Long pendingApplicantId) { this.pendingApplicantId = pendingApplicantId; }

    /**
     * Sets the status. A final status (VERIFIED, FAILED) clears {@code pendingApplicantId} so the applicant can start
     * a new verification. Does not check transitions itself; the service checks
     * {@link VerificationStatus#canTransitionTo} first.
     */
    public void setStatus(VerificationStatus status) {
        this.status = status;
        if (!status.isOpen()) {
            this.pendingApplicantId = null;
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
