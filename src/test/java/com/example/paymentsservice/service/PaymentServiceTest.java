package com.example.paymentsservice.service;

import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.domain.PaymentRepository;
import com.example.paymentsservice.domain.PaymentStatus;
import com.example.paymentsservice.exception.InvalidPaymentStateException;
import com.example.paymentsservice.exception.PaymentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @InjectMocks
    private PaymentService service;

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    /**
     * Builds a minimal Payment whose status is set to the supplied value.
     * The id field is left null (no setter exists) because the tests that need
     * an id inject it via repository stubs rather than relying on the field directly.
     */
    private Payment buildPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setDescription("Test payment");
        payment.setStatus(status);
        return payment;
    }

    /**
     * Convenience overload that also wires the payment up to a specific id by
     * configuring the repository stub so that findById(id) returns it.
     */
    private Payment buildPaymentWithId(Long id, PaymentStatus status) {
        Payment payment = buildPayment(status);
        when(repository.findById(id)).thenReturn(Optional.of(payment));
        return payment;
    }

    // ---------------------------------------------------------------------------
    // create()
    // ---------------------------------------------------------------------------

    @Test
    void create_setsStatusToAuthorizedAndSaves() {
        Payment incoming = buildPayment(null); // status not yet set
        Payment saved = buildPayment(PaymentStatus.AUTHORIZED);
        when(repository.save(any(Payment.class))).thenReturn(saved);

        Payment result = service.create(incoming);

        // Status must have been set to AUTHORIZED before saving
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        // Returned value is whatever the repository returns
        assertThat(result).isSameAs(saved);
    }

    // ---------------------------------------------------------------------------
    // findById()
    // ---------------------------------------------------------------------------

    @Test
    void findById_returnsPaymentWhenFound() {
        Payment payment = buildPayment(PaymentStatus.AUTHORIZED);
        when(repository.findById(1L)).thenReturn(Optional.of(payment));

        Payment result = service.findById(1L);

        assertThat(result).isSameAs(payment);
    }

    @Test
    void findById_throwsPaymentNotFoundExceptionWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.findById(99L));
    }

    // ---------------------------------------------------------------------------
    // findAll()
    // ---------------------------------------------------------------------------

    @Test
    void findAll_returnsListOfAllPayments() {
        List<Payment> payments = List.of(
                buildPayment(PaymentStatus.AUTHORIZED),
                buildPayment(PaymentStatus.CAPTURED)
        );
        when(repository.findAll()).thenReturn(payments);

        List<Payment> result = service.findAll();

        assertThat(result).hasSize(2).containsExactlyElementsOf(payments);
    }

    // ---------------------------------------------------------------------------
    // capture()
    // ---------------------------------------------------------------------------

    @Test
    void capture_transitionsAuthorizedToCaptured() {
        Payment payment = buildPaymentWithId(1L, PaymentStatus.AUTHORIZED);
        when(repository.save(payment)).thenReturn(payment);

        Payment result = service.capture(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(repository).save(payment);
    }

    @Test
    void capture_throwsInvalidPaymentStateExceptionWhenAlreadyCaptured() {
        buildPaymentWithId(2L, PaymentStatus.CAPTURED);

        assertThrows(InvalidPaymentStateException.class, () -> service.capture(2L));
        verify(repository, never()).save(any());
    }

    @Test
    void capture_throwsInvalidPaymentStateExceptionWhenRefunded() {
        buildPaymentWithId(3L, PaymentStatus.REFUNDED);

        assertThrows(InvalidPaymentStateException.class, () -> service.capture(3L));
        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------------------------
    // refund()
    // ---------------------------------------------------------------------------

    @Test
    void refund_transitionsCapturedToRefunded() {
        Payment payment = buildPaymentWithId(1L, PaymentStatus.CAPTURED);
        when(repository.save(payment)).thenReturn(payment);

        Payment result = service.refund(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(repository).save(payment);
    }

    @Test
    void refund_throwsInvalidPaymentStateExceptionWhenAuthorized() {
        buildPaymentWithId(2L, PaymentStatus.AUTHORIZED);

        assertThrows(InvalidPaymentStateException.class, () -> service.refund(2L));
        verify(repository, never()).save(any());
    }

    @Test
    void refund_throwsInvalidPaymentStateExceptionWhenAlreadyRefunded() {
        buildPaymentWithId(3L, PaymentStatus.REFUNDED);

        assertThrows(InvalidPaymentStateException.class, () -> service.refund(3L));
        verify(repository, never()).save(any());
    }
}
