package com.interview.app.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * MODEL layer: one {@code Item} object = one row in the {@code items} database table.
 *
 * <p>This is a JPA <b>entity</b>. JPA (Jakarta Persistence API) is the standard Java way to map classes
 * to tables; Hibernate is the library that implements it. Because {@code application.properties} sets
 * {@code spring.jpa.hibernate.ddl-auto=update}, Hibernate reads this class at startup and creates the
 * table for you. Roughly:
 * <pre>
 *   CREATE TABLE items (
 *     id          BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     name        VARCHAR(200)  NOT NULL,
 *     description VARCHAR(1000),
 *  *     created_at  TIMESTAMP     NOT NULL,
 *     updated_at  TIMESTAMP     NOT NULL,
 *     CONSTRAINT uk_items_name UNIQUE (name)
 *   );
 * </pre>
 * (Java field {@code createdAt} becomes column {@code created_at} automatically.)
 *
 * <p><b>Why rules live in the database too:</b> the unique constraint and NOT NULL columns are enforced
 * by the database itself. If two requests try to create the same name at the same moment, an
 * "if exists then reject" check in Java could let both through; the database cannot be raced.
 *
 * <p>Entities are never sent to or read from clients directly; the API uses the records in
 * {@code dto/ItemDtos} instead. Placeholder: rename or copy it for the interview's real entity.
 */
@Entity
@Table(name = "items",
        uniqueConstraints = @UniqueConstraint(name = "uk_items_name", columnNames = "name"))
public class Item {

    /** Primary key. IDENTITY = the database hands out 1, 2, 3... when the row is inserted. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Required, max 200 characters, unique across all items (see the table's unique constraint). */
    @Column(nullable = false, length = 200)
    private String name;

    /** Optional free text. */
    @Column(length = 1000)
    private String description;

    /** Set once on insert; {@code updatable = false} stops Hibernate from ever changing it. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Refreshed on every update. */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Empty constructor required by JPA: when Hibernate loads a row it first creates an empty object, then
     * fills in the fields. {@code protected} so normal code is forced to use the constructor below.
     */
    protected Item() { }

    /** The only way our own code creates an item: id and timestamps are never chosen by the caller. */
    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** JPA lifecycle hook: Hibernate calls this automatically just before the INSERT. */
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    /** JPA lifecycle hook: Hibernate calls this automatically just before an UPDATE. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters. Only fields that may change (name, description) have setters;
    // id and timestamps are managed by the database and the hooks above.
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
