package com.interview.app.verification;

import java.time.Instant;

/** DTOs: the JSON shapes the verification API returns. {@code pendingApplicantId} is internal and not exposed. */
public final class VerificationDtos {

    private VerificationDtos() { }

    public record VerificationResponse(Long id, Long applicantId, VerificationStatus status,
                                       Instant createdAt, Instant updatedAt) {

        public static VerificationResponse from(Verification v) {
            return new VerificationResponse(v.getId(), v.getApplicant().getId(), v.getStatus(),
                    v.getCreatedAt(), v.getUpdatedAt());
        }
    }
}
