package com.interview.app.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * API contract, separate from the JPA entity: clients can't set id, status or timestamps
 * (prevents mass assignment), and the DB schema can change without breaking the API.
 */
public final class ItemDtos {
    private ItemDtos() { }

    public record CreateItemRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description) { }

    public record UpdateItemRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description) { }

    public record StatusChangeRequest(
            @NotNull ItemStatus status,
            @Size(max = 500) String reason) { }

    public record ItemResponse(Long id, String name, String description, ItemStatus status,
                               Instant createdAt, Instant updatedAt) {
        static ItemResponse from(Item i) {
            return new ItemResponse(i.getId(), i.getName(), i.getDescription(), i.getStatus(),
                    i.getCreatedAt(), i.getUpdatedAt());
        }
    }

    public record ItemEventResponse(Long id, ItemStatus fromStatus, ItemStatus toStatus,
                                    String reason, Instant createdAt) {
        static ItemEventResponse from(ItemEvent e) {
            return new ItemEventResponse(e.getId(), e.getFromStatus(), e.getToStatus(),
                    e.getReason(), e.getCreatedAt());
        }
    }
}
