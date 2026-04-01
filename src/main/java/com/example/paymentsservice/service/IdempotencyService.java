package com.example.paymentsservice.service;

import com.example.paymentsservice.domain.IdempotencyRecord;
import com.example.paymentsservice.domain.IdempotencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Manages idempotency records for payment operations.
 *
 * <p>On the first call with a given {@code Idempotency-Key}, the caller
 * processes the request normally and invokes {@link #store} to persist the
 * outcome. On subsequent calls with the same key, {@link #find} returns the
 * stored record so the original response can be replayed without
 * reprocessing.
 */
@Service
@Transactional
public class IdempotencyService {

    private final IdempotencyRepository repository;

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey);
    }

    public IdempotencyRecord store(String idempotencyKey, String requestPath,
                                   String responseBody, int httpStatus) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestPath(requestPath);
        record.setResponseBody(responseBody);
        record.setHttpStatus(httpStatus);
        return repository.save(record);
    }
}
