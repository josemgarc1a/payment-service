package com.example.paymentsservice.api;

import com.example.paymentsservice.domain.IdempotencyRecord;
import com.example.paymentsservice.domain.Payment;
import com.example.paymentsservice.domain.PaymentStatus;
import com.example.paymentsservice.exception.InvalidPaymentStateException;
import com.example.paymentsservice.exception.PaymentNotFoundException;
import com.example.paymentsservice.service.IdempotencyService;
import com.example.paymentsservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private IdempotencyService idempotencyService;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Payment buildPayment(Long id, PaymentStatus status) {
        Payment p = new Payment() {
            @Override public Long getId() { return id; }
            @Override public Instant getCreatedAt() { return Instant.parse("2026-04-01T00:00:00Z"); }
        };
        p.setAmount(new BigDecimal("100.00"));
        p.setCurrency("USD");
        p.setDescription("Test payment");
        p.setStatus(status);
        return p;
    }

    private IdempotencyRecord buildIdempotencyRecord(String key, String responseBody, int status) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setRequestPath("/payments");
        record.setResponseBody(responseBody);
        record.setHttpStatus(status);
        return record;
    }

    // =========================================================================
    // POST /payments
    // =========================================================================

    @Test
    void createPayment_validRequest_returns201WithBody() throws Exception {
        Payment created = buildPayment(1L, PaymentStatus.AUTHORIZED);
        when(idempotencyService.find(any())).thenReturn(Optional.empty());
        when(paymentService.create(any(Payment.class))).thenReturn(created);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"100.00","currency":"USD","description":"Test payment"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void createPayment_duplicateIdempotencyKey_replaysOriginalResponse() throws Exception {
        String storedJson = """
                {"id":1,"amount":100.00,"currency":"USD","description":"Test payment",
                 "status":"AUTHORIZED","createdAt":"2026-04-01T00:00:00Z"}
                """;
        when(idempotencyService.find("key-123"))
                .thenReturn(Optional.of(buildIdempotencyRecord("key-123", storedJson, 201)));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"100.00","currency":"USD","description":"Test payment"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void createPayment_missingAmount_returns400WithAmountField() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD","description":"Test payment"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void createPayment_negativeAmount_returns400WithAmountField() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"-1.00","currency":"USD","description":"Test payment"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void createPayment_blankDescription_returns400WithDescriptionField() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"100.00","currency":"USD","description":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void createPayment_currencyNotThreeChars_returns400WithCurrencyField() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"100.00","currency":"US","description":"Test payment"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.currency").exists());
    }

    // =========================================================================
    // GET /payments
    // =========================================================================

    @Test
    void findAll_returns200WithListOfPayments() throws Exception {
        when(paymentService.findAll()).thenReturn(List.of(
                buildPayment(1L, PaymentStatus.AUTHORIZED),
                buildPayment(2L, PaymentStatus.CAPTURED)));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    // =========================================================================
    // GET /payments/{id}
    // =========================================================================

    @Test
    void findById_found_returns200WithPaymentBody() throws Exception {
        when(paymentService.findById(42L)).thenReturn(buildPayment(42L, PaymentStatus.AUTHORIZED));

        mockMvc.perform(get("/payments/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void findById_notFound_returns404WithErrorField() throws Exception {
        when(paymentService.findById(99L)).thenThrow(new PaymentNotFoundException(99L));

        mockMvc.perform(get("/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // =========================================================================
    // POST /payments/{id}/capture
    // =========================================================================

    @Test
    void capture_success_returns200WithCapturedStatus() throws Exception {
        when(paymentService.capture(1L)).thenReturn(buildPayment(1L, PaymentStatus.CAPTURED));

        mockMvc.perform(post("/payments/1/capture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));
    }

    @Test
    void capture_notFound_returns404WithErrorField() throws Exception {
        when(paymentService.capture(99L)).thenThrow(new PaymentNotFoundException(99L));

        mockMvc.perform(post("/payments/99/capture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void capture_invalidState_returns409WithErrorField() throws Exception {
        when(paymentService.capture(1L))
                .thenThrow(new InvalidPaymentStateException("Cannot capture payment 1 in status CAPTURED"));

        mockMvc.perform(post("/payments/1/capture"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    // =========================================================================
    // POST /payments/{id}/refund
    // =========================================================================

    @Test
    void refund_success_returns200WithRefundedStatus() throws Exception {
        when(idempotencyService.find(any())).thenReturn(Optional.empty());
        when(paymentService.refund(1L)).thenReturn(buildPayment(1L, PaymentStatus.REFUNDED));

        mockMvc.perform(post("/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void refund_duplicateIdempotencyKey_replaysOriginalResponse() throws Exception {
        String storedJson = """
                {"id":1,"amount":100.00,"currency":"USD","description":"Test payment",
                 "status":"REFUNDED","createdAt":"2026-04-01T00:00:00Z"}
                """;
        when(idempotencyService.find("refund-key-abc"))
                .thenReturn(Optional.of(buildIdempotencyRecord("refund-key-abc", storedJson, 200)));

        mockMvc.perform(post("/payments/1/refund")
                        .header("Idempotency-Key", "refund-key-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void refund_notFound_returns404WithErrorField() throws Exception {
        when(idempotencyService.find(any())).thenReturn(Optional.empty());
        when(paymentService.refund(99L)).thenThrow(new PaymentNotFoundException(99L));

        mockMvc.perform(post("/payments/99/refund"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void refund_invalidState_returns409WithErrorField() throws Exception {
        when(idempotencyService.find(any())).thenReturn(Optional.empty());
        when(paymentService.refund(1L))
                .thenThrow(new InvalidPaymentStateException("Cannot refund payment 1 in status AUTHORIZED"));

        mockMvc.perform(post("/payments/1/refund"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
