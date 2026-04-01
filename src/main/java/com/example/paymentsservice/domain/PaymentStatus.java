package com.example.paymentsservice.domain;

/**
 * Represents the lifecycle states of a {@link Payment}.
 *
 * <p>Valid state transitions are:
 * <pre>
 *   AUTHORIZED --capture--> CAPTURED --refund--> REFUNDED
 *   AUTHORIZED --cancel-->  CANCELLED
 * </pre>
 *
 * {@code CANCELLED} and {@code REFUNDED} are terminal states; no further
 * transitions are permitted once a payment reaches either of them.
 */
public enum PaymentStatus {
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    CANCELLED
}
