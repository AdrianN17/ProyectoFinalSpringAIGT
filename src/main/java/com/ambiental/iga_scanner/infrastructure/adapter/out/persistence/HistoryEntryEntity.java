package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "history")
class HistoryEntryEntity {

    @Id
    @Column(name = "trace_id")
    private UUID traceId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Lob
    @Column(name = "question", nullable = false)
    private String question;

    @Lob
    @Column(name = "answer", nullable = false)
    private String answer;

    // "user" is a reserved word in T-SQL; backticks tell Hibernate to quote it per-dialect.
    @Column(name = "`user`")
    private String user;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Lob
    @Column(name = "retrieved_contexts")
    private String retrievedContexts;

    protected HistoryEntryEntity() {
    }

    HistoryEntryEntity(UUID traceId, UUID sessionId, String question, String answer, String user,
            Integer inputTokens, Integer outputTokens, Instant createdAt, String retrievedContexts) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.question = question;
        this.answer = answer;
        this.user = user;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.createdAt = createdAt;
        this.retrievedContexts = retrievedContexts;
    }

    UUID getTraceId() {
        return traceId;
    }

    UUID getSessionId() {
        return sessionId;
    }

    String getQuestion() {
        return question;
    }

    String getAnswer() {
        return answer;
    }

    String getUser() {
        return user;
    }

    Integer getInputTokens() {
        return inputTokens;
    }

    Integer getOutputTokens() {
        return outputTokens;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    String getRetrievedContexts() {
        return retrievedContexts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistoryEntryEntity other)) {
            return false;
        }
        return traceId != null && traceId.equals(other.traceId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
