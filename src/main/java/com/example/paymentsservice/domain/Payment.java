package com.example.paymentsservice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persistent entity representing a single payment record.
 *
 * <p>A payment begins in the {@link PaymentStatus#AUTHORIZED} state and advances
 * through the state machine enforced by {@link com.example.paymentsservice.service.PaymentService}.
 * The {@code createdAt} timestamp is set automatically on first persist and is
 * never updated thereafter.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JPA optimistic-locking token. Prevents lost updates when two concurrent
     * requests attempt to transition the same payment to a new state at the
     * same time; the second writer will receive an {@code OptimisticLockException}.
     */
    @Version
    private Long version;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Automatically assigns {@code createdAt} to the current UTC instant on first persist. */
    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public Long getVersion() { return version; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
