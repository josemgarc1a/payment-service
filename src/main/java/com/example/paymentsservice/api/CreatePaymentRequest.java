package com.example.paymentsservice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Inbound DTO for {@code POST /payments}.
 *
 * <p>Bean Validation constraints are evaluated by Spring MVC before the request reaches
 * the service layer:
 * <ul>
 *   <li>{@code amount} must be non-null and strictly positive.</li>
 *   <li>{@code currency} must be a non-blank, exactly 3-character ISO 4217 code
 *       (e.g. {@code USD}, {@code EUR}).</li>
 *   <li>{@code description} must be non-blank.</li>
 * </ul>
 * Validation failures are handled by {@link GlobalExceptionHandler} and returned
 * as HTTP 400 with per-field error messages.
 */
public class CreatePaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank
    private String description;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
