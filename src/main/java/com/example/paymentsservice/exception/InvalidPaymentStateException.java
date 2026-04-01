package com.example.paymentsservice.exception;

/**
 * Thrown by {@link com.example.paymentsservice.service.PaymentService} when a
 * requested state transition is not permitted for the payment's current status.
 *
 * <p>For example, attempting to capture a payment that is already
 * {@code CAPTURED}, {@code REFUNDED}, or {@code CANCELLED} will trigger this
 * exception. It is an unchecked exception and is expected to map to an HTTP
 * 409 Conflict response at the API layer.
 */
public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
