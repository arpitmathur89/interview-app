package com.interview.app.applicant;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MODEL: one {@code Applicant} = one row in the {@code applicants} table.
 * <pre>
 *   CREATE TABLE applicants (
 *     id            BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     name          VARCHAR(200)   NOT NULL,
 *     email         VARCHAR(254)   NOT NULL,
 *     employer      VARCHAR(200)   NOT NULL,
 *     annual_income NUMERIC(19,2)  NOT NULL,
 *     currency      VARCHAR(3)     NOT NULL,
 *     created_at    TIMESTAMP      NOT NULL,
 *     updated_at    TIMESTAMP      NOT NULL
 *   );
 * </pre>
 * Money is a {@code BigDecimal} (exact), never {@code double}. {@code currency} is set server side to "USD";
 * clients can't send it.
 */
@Entity
@Table(name = "applicants")
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false,length = 200)
    private String employer;

    /** Exact decimal: up to 17 digits before the point and 2 after. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal annualIncome;

    /** Defaulted server side; there is no setter, so it can't be changed through the API. */
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Empty constructor required by JPA; {@code protected} so normal code uses the one below. */
    protected Applicant() { }

    public Applicant(String name, String email, String employer, BigDecimal annualIncome) {
        this.name = name;
        this.email = email;
        this.employer = employer;
        this.annualIncome = annualIncome;
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }
    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
