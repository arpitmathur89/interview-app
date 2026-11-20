package com.interview.app.item;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle states plus the allowed transitions, kept in one place so the rule is easy to explain and change.
 * Rename the states to match the interview prompt (e.g. PENDING / VERIFIED / NEEDS_MORE_INFO).
 */
public enum ItemStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED;

    private static final Map<ItemStatus, Set<ItemStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(IN_REVIEW, REJECTED),
            IN_REVIEW, EnumSet.of(APPROVED, REJECTED, PENDING),
            APPROVED, EnumSet.noneOf(ItemStatus.class),   // terminal
            REJECTED, EnumSet.noneOf(ItemStatus.class)    // terminal
    );

    public boolean canTransitionTo(ItemStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
