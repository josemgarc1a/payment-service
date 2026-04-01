package com.example.paymentsservice.api;

import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.domain.IdempotencyRecord;
import com.example.paymentsservice.service.IdempotencyService;
import com.example.paymentsservice.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller exposing the five payment lifecycle endpoints:
 * <ol>
 *   <li>{@code POST   /payments}              - create a new payment</li>
 *   <li>{@code GET    /payments}              - list all payments</li>
 *   <li>{@code GET    /payments/{id}}         - retrieve a single payment</li>
 *   <li>{@code POST   /payments/{id}/capture} - capture an authorised payment</li>
 *   <li>{@code POST   /payments/{id}/refund}  - refund a captured payment</li>
 * </ol>
 *
 * <p>All responses use {@link PaymentResponse} to decouple the API contract from the
 * domain model. Validation errors and domain exceptions are handled globally by
 * {@link GlobalExceptionHandler}.
 *
 * <p>The {@code POST /payments} and {@code POST /payments/{id}/refund} endpoints
 * support idempotency via the {@code Idempotency-Key} request header. Sending the
 * same key on a retry replays the original response without reprocessing.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService service,
                             IdempotencyService idempotencyService,
                             ObjectMapper objectMapper) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) throws JsonProcessingException {

        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> existing = idempotencyService.find(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                PaymentResponse response = objectMapper.readValue(record.getResponseBody(), PaymentResponse.class);
                return ResponseEntity.status(record.getHttpStatus()).body(response);
            }
        }

        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        PaymentResponse response = PaymentResponse.from(service.create(payment));

        if (idempotencyKey != null) {
            idempotencyService.store(idempotencyKey, "/payments",
                    objectMapper.writeValueAsString(response), HttpStatus.CREATED.value());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<PaymentResponse> findAll() {
        return service.findAll().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable Long id) {
        return PaymentResponse.from(service.findById(id));
    }

    /**
     * Captures an authorised payment, moving it to the {@code CAPTURED} state.
     *
     * <p>Returns HTTP 409 Conflict if the payment is not in a state that permits
     * capture (e.g. already captured, refunded, or cancelled). The conflict is
     * raised by the service layer as an {@link com.example.paymentsservice.exception.InvalidPaymentStateException}
     * and mapped to 409 by {@link GlobalExceptionHandler}.
     *
     * @param id the payment identifier
     * @return the updated payment
     */
    @PostMapping("/{id}/capture")
    public PaymentResponse capture(@PathVariable Long id) {
        return PaymentResponse.from(service.capture(id));
    }

    /**
     * Refunds a previously captured payment, moving it to the {@code REFUNDED} state.
     *
     * <p>Returns HTTP 409 Conflict if the payment is not in the {@code CAPTURED} state.
     * Supports idempotency via the {@code Idempotency-Key} header — replaying the same
     * key returns the original response without issuing a second refund.
     *
     * @param id the payment identifier
     * @return the updated payment
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey)
            throws JsonProcessingException {

        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> existing = idempotencyService.find(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                PaymentResponse response = objectMapper.readValue(record.getResponseBody(), PaymentResponse.class);
                return ResponseEntity.status(record.getHttpStatus()).body(response);
            }
        }

        PaymentResponse response = PaymentResponse.from(service.refund(id));

        if (idempotencyKey != null) {
            idempotencyService.store(idempotencyKey, "/payments/" + id + "/refund",
                    objectMapper.writeValueAsString(response), HttpStatus.OK.value());
        }

        return ResponseEntity.ok(response);
    }
}
