package za.ac.skiet.vyou2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable end-of-interview summary. Average scores are computed locally from the turns;
 * the narrative feedback comes from the LLM. One per session.
 */
@Entity
@Table(name = "interview_summaries")
public class InterviewSummary {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID sessionId;

    @Column(nullable = false)
    private int questionsAnswered;

    private double averageClarity;
    private double averageTechnical;
    private double averageConfidence;
    private double averageOverall;

    @Column(nullable = false, length = 4000)
    private String feedback;

    @Column(length = 4000)
    private String topStrengths;

    @Column(length = 4000)
    private String focusAreas;

    @Column(nullable = false)
    private Instant createdAt;

    protected InterviewSummary() {
        // for JPA only
    }

    public InterviewSummary(UUID id, UUID sessionId, int questionsAnswered, double averageClarity,
                            double averageTechnical, double averageConfidence, double averageOverall,
                            String feedback, String topStrengths, String focusAreas, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionsAnswered = questionsAnswered;
        this.averageClarity = averageClarity;
        this.averageTechnical = averageTechnical;
        this.averageConfidence = averageConfidence;
        this.averageOverall = averageOverall;
        this.feedback = feedback;
        this.topStrengths = topStrengths;
        this.focusAreas = focusAreas;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getQuestionsAnswered() {
        return questionsAnswered;
    }

    public double getAverageClarity() {
        return averageClarity;
    }

    public double getAverageTechnical() {
        return averageTechnical;
    }

    public double getAverageConfidence() {
        return averageConfidence;
    }

    public double getAverageOverall() {
        return averageOverall;
    }

    public String getFeedback() {
        return feedback;
    }

    /** Newline-separated list. */
    public String getTopStrengths() {
        return topStrengths;
    }

    /** Newline-separated list. */
    public String getFocusAreas() {
        return focusAreas;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
