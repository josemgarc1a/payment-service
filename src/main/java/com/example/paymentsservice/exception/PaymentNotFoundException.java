package com.example.paymentsservice.exception;

/**
 * Thrown by {@link com.example.paymentsservice.service.PaymentService} when a
 * payment lookup by ID produces no result.
 *
 * <p>It is an unchecked exception and is expected to map to an HTTP 404 Not
 * Found response at the API layer.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Payment not found: " + id);
    }
}
