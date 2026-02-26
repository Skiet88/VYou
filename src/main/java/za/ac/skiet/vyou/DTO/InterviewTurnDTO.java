package za.ac.skiet.vyou.DTO;

import java.util.UUID;

public class InterviewTurnDTO {

    private UUID id;

    // session (ONLY SAFE FIELDS)
    private UUID interviewSessionId;

    private Integer sequenceNumber;

    // question
    private UUID questionContentId;
    private String questionText;

    // answer
    private UUID answerContentId;
    private String answerText;

    // AI evaluation
    private UUID aiEvaluationId;
    private Integer aiOverallScore;
    private String aiStrengths;
    private String aiImprovements;

    // getters & setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInterviewSessionId() { return interviewSessionId; }
    public void setInterviewSessionId(UUID interviewSessionId) {
        this.interviewSessionId = interviewSessionId;
    }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public UUID getQuestionContentId() { return questionContentId; }
    public void setQuestionContentId(UUID questionContentId) {
        this.questionContentId = questionContentId;
    }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public UUID getAnswerContentId() { return answerContentId; }
    public void setAnswerContentId(UUID answerContentId) {
        this.answerContentId = answerContentId;
    }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public UUID getAiEvaluationId() { return aiEvaluationId; }
    public void setAiEvaluationId(UUID aiEvaluationId) {
        this.aiEvaluationId = aiEvaluationId;
    }

    public Integer getAiOverallScore() { return aiOverallScore; }
    public void setAiOverallScore(Integer aiOverallScore) {
        this.aiOverallScore = aiOverallScore;
    }

    public String getAiStrengths() { return aiStrengths; }
    public void setAiStrengths(String aiStrengths) {
        this.aiStrengths = aiStrengths;
    }

    public String getAiImprovements() { return aiImprovements; }
    public void setAiImprovements(String aiImprovements) {
        this.aiImprovements = aiImprovements;
    }

    @Override
    public String toString() {
        return "InterviewTurnDTO{" +
                "id=" + id +
                ", interviewSessionId=" + interviewSessionId +
                ", sequenceNumber=" + sequenceNumber +
                ", questionContentId=" + questionContentId +
                ", questionText='" + questionText + '\'' +
                ", answerContentId=" + answerContentId +
                ", answerText='" + answerText + '\'' +
                ", aiEvaluationId=" + aiEvaluationId +
                ", aiOverallScore=" + aiOverallScore +
                ", aiStrengths='" + aiStrengths + '\'' +
                ", aiImprovements='" + aiImprovements + '\'' +
                '}';
    }
}
