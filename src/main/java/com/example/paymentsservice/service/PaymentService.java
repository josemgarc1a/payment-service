package com.example.paymentsservice.service;

import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.domain.PaymentRepository;
import com.example.paymentsservice.domain.PaymentStatus;
import com.example.paymentsservice.exception.InvalidPaymentStateException;
import com.example.paymentsservice.exception.PaymentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service that manages the full lifecycle of a {@link Payment}.
 *
 * <p>This service is the single authority for state-machine transitions.
 * Allowed transitions are:
 * <pre>
 *   AUTHORIZED --capture--> CAPTURED --refund--> REFUNDED
 *   AUTHORIZED --cancel-->  CANCELLED
 * </pre>
 * Any attempt to perform a transition that is not valid for the payment's
 * current state results in an {@link com.example.paymentsservice.exception.InvalidPaymentStateException}.
 *
 * <p>All mutating operations run inside a transaction. Read-only operations
 * use {@code @Transactional(readOnly = true)} to allow connection-pool and
 * replica optimisations.
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists a new payment and sets its initial status to
     * {@link PaymentStatus#AUTHORIZED}, overriding any status supplied by the
     * caller to ensure every payment begins at the correct starting state.
     *
     * @param payment the payment to create (status field will be overwritten)
     * @return the saved payment with its generated {@code id} and {@code createdAt}
     */
    public Payment create(Payment payment) {
        payment.setStatus(PaymentStatus.AUTHORIZED);
        return repository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return repository.findAll();
    }

    /**
     * Transitions a payment from {@link PaymentStatus#AUTHORIZED} to
     * {@link PaymentStatus#CAPTURED}.
     *
     * <p>Guard condition: the payment must currently be in the {@code AUTHORIZED}
     * state. If it is in any other state (e.g. already captured, refunded, or
     * cancelled) an {@link InvalidPaymentStateException} is thrown and no
     * change is persisted.
     *
     * @param id the identifier of the payment to capture
     * @return the updated payment with status {@code CAPTURED}
     * @throws com.example.paymentsservice.exception.PaymentNotFoundException if no payment with {@code id} exists
     * @throws InvalidPaymentStateException if the payment is not in {@code AUTHORIZED} status
     */
    public Payment capture(Long id) {
        Payment payment = findById(id);
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException(
                    "Cannot capture payment " + id + " in status " + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.CAPTURED);
        return repository.save(payment);
    }

    /**
     * Transitions a payment from {@link PaymentStatus#CAPTURED} to
     * {@link PaymentStatus#REFUNDED}.
     *
     * <p>Guard condition: the payment must currently be in the {@code CAPTURED}
     * state. Attempting to refund an {@code AUTHORIZED}, {@code REFUNDED}, or
     * {@code CANCELLED} payment throws an {@link InvalidPaymentStateException}
     * and leaves the record unchanged.
     *
     * @param id the identifier of the payment to refund
     * @return the updated payment with status {@code REFUNDED}
     * @throws com.example.paymentsservice.exception.PaymentNotFoundException if no payment with {@code id} exists
     * @throws InvalidPaymentStateException if the payment is not in {@code CAPTURED} status
     */
    public Payment refund(Long id) {
        Payment payment = findById(id);
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new InvalidPaymentStateException(
                    "Cannot refund payment " + id + " in status " + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        return repository.save(payment);
    }
}
