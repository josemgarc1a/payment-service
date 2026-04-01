package com.example.paymentsservice.api;

import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing the five payment lifecycle endpoints:
 * <ol>
 *   <li>{@code POST   /payments}          - create a new payment</li>
 *   <li>{@code GET    /payments}          - list all payments</li>
 *   <li>{@code GET    /payments/{id}}     - retrieve a single payment</li>
 *   <li>{@code POST   /payments/{id}/capture} - capture an authorised payment</li>
 *   <li>{@code POST   /payments/{id}/refund}  - refund a captured payment</li>
 * </ol>
 *
 * <p>All responses use {@link PaymentResponse} to decouple the API contract from the
 * domain model. Validation errors and domain exceptions are handled globally by
 * {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        return PaymentResponse.from(service.create(payment));
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
     * <p>Returns HTTP 409 Conflict if the payment is not in the {@code CAPTURED} state
     * (e.g. still pending, already refunded, or cancelled). The conflict is raised by
     * the service layer as an {@link com.example.paymentsservice.exception.InvalidPaymentStateException}
     * and mapped to 409 by {@link GlobalExceptionHandler}.
     *
     * @param id the payment identifier
     * @return the updated payment
     */
    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable Long id) {
        return PaymentResponse.from(service.refund(id));
    }
}
