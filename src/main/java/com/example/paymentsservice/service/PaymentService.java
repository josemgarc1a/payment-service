package com.example.paymentsservice.service;

import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.domain.PaymentRepository;
import com.example.paymentsservice.domain.PaymentStatus;
import com.example.paymentsservice.exception.InvalidPaymentStateException;
import com.example.paymentsservice.exception.PaymentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

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

    public Payment capture(Long id) {
        Payment payment = findById(id);
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException(
                    "Cannot capture payment " + id + " in status " + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.CAPTURED);
        return repository.save(payment);
    }

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
