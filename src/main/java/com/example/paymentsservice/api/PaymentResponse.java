package com.example.paymentsservice.api;

import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound DTO returned by all payment endpoints.
 *
 * <p>The {@link com.example.paymentsservice.domain.Payment} entity is never serialised
 * directly to the API layer. This class acts as an explicit contract boundary,
 * ensuring internal domain changes do not accidentally alter the public response shape.
 */
public class PaymentResponse {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private String description;
    private PaymentStatus status;
    private Instant createdAt;

    /**
     * Creates a {@code PaymentResponse} from the given domain entity.
     *
     * <p>Only the fields that form the public API contract are copied; any
     * persistence or audit-only fields on {@link Payment} are intentionally omitted.
     *
     * @param payment the domain entity to convert; must not be {@code null}
     * @return a fully populated {@code PaymentResponse}
     */
    public static PaymentResponse from(Payment payment) {
        PaymentResponse r = new PaymentResponse();
        r.id = payment.getId();
        r.amount = payment.getAmount();
        r.currency = payment.getCurrency();
        r.description = payment.getDescription();
        r.status = payment.getStatus();
        r.createdAt = payment.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
