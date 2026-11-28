package com.interview.app.verification;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State of a verification, plus the allowed moves:
 * <pre>
 *   PENDING      --> VERIFIED, NEEDS_REVIEW, FAILED
 *   NEEDS_REVIEW --> VERIFIED, FAILED
 *   VERIFIED     --> (none: final)
 *   FAILED       --> (none: final)
 * </pre>
 * PENDING and NEEDS_REVIEW are "open" (in progress); an applicant may have only one open verification at a time.
 */
public enum VerificationStatus {
    PENDING, VERIFIED, NEEDS_REVIEW, FAILED;

    private static final Map<VerificationStatus, Set<VerificationStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(VERIFIED, NEEDS_REVIEW, FAILED),
            NEEDS_REVIEW, EnumSet.of(VERIFIED, FAILED),
            VERIFIED, EnumSet.noneOf(VerificationStatus.class),
            FAILED, EnumSet.noneOf(VerificationStatus.class)
    );

    /** Is moving from this status to {@code next} allowed? An illegal move should become a 409 in the service. */
    public boolean canTransitionTo(VerificationStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** True while the verification is still in progress (not yet VERIFIED or FAILED). */
    public boolean isOpen() {
        return this == PENDING || this == NEEDS_REVIEW;
    }
}
