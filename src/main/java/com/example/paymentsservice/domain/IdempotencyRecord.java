package com.example.paymentsservice.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persists the outcome of an idempotent request so that replays with the
 * same {@code Idempotency-Key} header return the original response without
 * re-executing the operation.
 */
@Entity
@Table(name = "idempotency_records",
       uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String requestPath;

    /** Serialised JSON of the original PaymentResponse. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false)
    private int httpStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }

    public Instant getCreatedAt() { return createdAt; }
}
