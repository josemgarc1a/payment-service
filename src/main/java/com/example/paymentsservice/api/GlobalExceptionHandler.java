package com.example.paymentsservice.api;

import com.example.paymentsservice.exception.InvalidPaymentStateException;
import com.example.paymentsservice.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for the payments REST API.
 *
 * <p>Maps domain and validation exceptions to appropriate HTTP status codes:
 * <ul>
 *   <li>{@link com.example.paymentsservice.exception.PaymentNotFoundException}
 *       &rarr; {@code 404 Not Found}</li>
 *   <li>{@link com.example.paymentsservice.exception.InvalidPaymentStateException}
 *       &rarr; {@code 409 Conflict} (e.g. capturing an already-captured payment)</li>
 *   <li>{@link org.springframework.web.bind.MethodArgumentNotValidException}
 *       &rarr; {@code 400 Bad Request} with a per-field error map</li>
 * </ul>
 * Individual handler methods are self-documenting via their {@code @ResponseStatus}
 * annotations and are therefore not documented separately.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(PaymentNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleInvalidState(InvalidPaymentStateException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid",
                        (a, b) -> a));
    }
}
