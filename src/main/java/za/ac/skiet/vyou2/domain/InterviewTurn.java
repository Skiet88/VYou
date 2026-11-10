package za.ac.skiet.vyou2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** Immutable question/answer/evaluation record. Turns are append-only. */
@Entity
@Table(name = "interview_turns",
        uniqueConstraints = @UniqueConstraint(name = "uk_turns_session_sequence", columnNames = {"session_id", "sequence_number"}))
public class InterviewTurn {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int sequenceNumber;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 10000)
    private String answer;

    @Embedded
    private Evaluation evaluation;

    @Column(nullable = false)
    private Instant answeredAt;

    protected InterviewTurn() {
        // for JPA only
    }

    public InterviewTurn(UUID id, UUID sessionId, int sequenceNumber, String question, String answer,
                         Evaluation evaluation, Instant answeredAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
        this.question = question;
        this.answer = answer;
        this.evaluation = evaluation;
        this.answeredAt = answeredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
