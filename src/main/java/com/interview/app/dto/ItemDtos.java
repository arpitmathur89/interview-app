package com.interview.app.dto;

import com.interview.app.model.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs (Data Transfer Objects): the exact JSON shapes the API accepts and returns.
 * This file is the API <b>contract</b>.
 *
 * <p><b>Why not just send/receive the {@code Item} entity?</b>
 * <ul>
 *   <li><b>Safety (mass assignment):</b> {@code CreateItemRequest} has no {@code id} or timestamp fields,
 *       so a client sending {@code {"id":7,"name":"x"}} cannot choose its own id; the unknown key is
 *       simply ignored.</li>
 *   <li><b>Freedom to change:</b> the table can gain columns without changing what clients see.</li>
 *   <li><b>No surprises:</b> returning entities can trigger lazy-loading errors or leak internal fields.</li>
 * </ul>
 *
 * <p>Each DTO is a Java {@code record}: a compact class that only holds data. Writing
 * {@code record CreateItemRequest(String name, String description)} gives you a constructor, the
 * accessors {@code name()} and {@code description()}, plus {@code equals}/{@code hashCode}/{@code toString}.
 * Jackson (the JSON library inside Spring) converts JSON to records and records to JSON automatically.
 *
 * <p>The annotations on record fields ({@code @NotBlank}, {@code @Size}) are
 * <b>Bean Validation</b> rules. They run when the controller parameter is marked {@code @Valid}; any
 * failure becomes a 422 response listing the bad fields (see {@code ApiExceptionHandler.validation}).
 *
 * <p>The records are grouped as nested types in one file to keep the scaffold small; they are used as
 * {@code ItemDtos.CreateItemRequest} or, after {@code import ...ItemDtos.*}, just {@code CreateItemRequest}.
 */
public final class ItemDtos {

    /** Private constructor: this class is only a container for the records and is never instantiated. */
    private ItemDtos() { }

    /**
     * Body of {@code POST /api/items}. Example: {@code {"name": "alpha", "description": "first one"}}.
     * {@code name} must contain a non-space character and be at most 200 chars (matching the DB column).
     */
    public record CreateItemRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description) { }

    /**
     * Body of {@code PUT /api/items/{id}}. PUT replaces the editable fields, so {@code name} is required
     * again; sending no description clears it.
     */
    public record UpdateItemRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description) { }

    /** What the API returns for an item. This is the only place that decides which entity fields are public. */
    public record ItemResponse(Long id, String name, String description,
                               Instant createdAt, Instant updatedAt) {

        /** Copies an entity into the response shape. Used as {@code ItemResponse::from} in the service. */
        public static ItemResponse from(Item i) {
            return new ItemResponse(i.getId(), i.getName(), i.getDescription(),
                    i.getCreatedAt(), i.getUpdatedAt());
        }
    }
}
