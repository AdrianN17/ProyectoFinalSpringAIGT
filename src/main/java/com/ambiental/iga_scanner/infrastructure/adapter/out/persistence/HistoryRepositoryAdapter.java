package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.persistence.HistoryRepositoryAdapterMessageError.QUERY_FAILED;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.persistence.HistoryRepositoryAdapterMessageError.SAVE_FAILED;

import com.ambiental.iga_scanner.application.port.out.HistoryRepositoryPort;
import com.ambiental.iga_scanner.domain.HistoryTrace;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
class HistoryRepositoryAdapter implements HistoryRepositoryPort {

    private final HistoryJpaRepository jpaRepository;

    HistoryRepositoryAdapter(HistoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public HistoryTrace save(HistoryTrace trace) {
        var entity = new HistoryEntryEntity(
                trace.traceId(),
                trace.sessionId(),
                trace.question(),
                trace.answer(),
                trace.user(),
                trace.inputTokens(),
                trace.outputTokens(),
                trace.createdAt(),
                trace.retrievedContexts());
        try {
            jpaRepository.save(entity);
        } catch (DataAccessException e) {
            throw new PersistenceException(SAVE_FAILED, e);
        }
        return trace;
    }

    @Override
    public List<HistoryTrace> findBySessionId(UUID sessionId) {
        try {
            return jpaRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                    .map(entity -> new HistoryTrace(
                            entity.getTraceId(),
                            entity.getSessionId(),
                            entity.getQuestion(),
                            entity.getAnswer(),
                            entity.getUser(),
                            entity.getInputTokens(),
                            entity.getOutputTokens(),
                            entity.getCreatedAt(),
                            entity.getRetrievedContexts()))
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceException(QUERY_FAILED, e);
        }
    }
}
