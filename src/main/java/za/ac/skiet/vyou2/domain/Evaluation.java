package za.ac.skiet.vyou2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Immutable AI evaluation of a single answer. Create through {@code EvaluationFactory}. */
@Embeddable
public class Evaluation {

    private int clarityScore;
    private int technicalScore;
    private int confidenceScore;
    private int overallScore;

    @Column(length = 4000)
    private String strengths;

    @Column(length = 4000)
    private String improvements;

    protected Evaluation() {
        // for JPA only
    }

    public Evaluation(int clarityScore, int technicalScore, int confidenceScore, int overallScore,
                      String strengths, String improvements) {
        this.clarityScore = clarityScore;
        this.technicalScore = technicalScore;
        this.confidenceScore = confidenceScore;
        this.overallScore = overallScore;
        this.strengths = strengths;
        this.improvements = improvements;
    }

    public int getClarityScore() {
        return clarityScore;
    }

    public int getTechnicalScore() {
        return technicalScore;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public int getOverallScore() {
        return overallScore;
    }

    /** Newline-separated list. */
    public String getStrengths() {
        return strengths;
    }

    /** Newline-separated list. */
    public String getImprovements() {
        return improvements;
    }
}
