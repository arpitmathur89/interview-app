package com.interview.app.item;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Append-only audit trail of status changes (child table with a FK to items).
 * ON DELETE CASCADE: deleting an item removes its history. For regulated data you'd
 * soft-delete the item instead and keep the history.
 */
@Entity
@Table(name = "item_events", indexes = @Index(name = "ix_item_events_item_id", columnList = "item_id"))
public class ItemEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus toStatus;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ItemEvent() { } // for JPA

    public ItemEvent(Item item, ItemStatus fromStatus, ItemStatus toStatus, String reason) {
        this.item = item;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public ItemStatus getFromStatus() { return fromStatus; }
    public ItemStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
