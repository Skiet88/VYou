package za.ac.skiet.vyou.DTO;

import za.ac.skiet.vyou.ENUM.InterviewStatus;
import za.ac.skiet.vyou.ENUM.InterviewMode;

import java.time.LocalDateTime;
import java.util.UUID;

public class InterviewSummaryDTO {
    private UUID id;
    private UUID sessionId;            // only include the session ID
    private String sessionJobRole;     // optional, if you want more info
    private String sessionLevel;       // optional
    private InterviewMode sessionMode; // optional
    private InterviewStatus sessionStatus; // optional

    private Integer overallScore;
    private String generalFeedback;
    private String recommendations;
    private LocalDateTime createdAt;

    // getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public String getSessionJobRole() { return sessionJobRole; }
    public void setSessionJobRole(String sessionJobRole) { this.sessionJobRole = sessionJobRole; }
    public String getSessionLevel() { return sessionLevel; }
    public void setSessionLevel(String sessionLevel) { this.sessionLevel = sessionLevel; }
    public InterviewMode getSessionMode() { return sessionMode; }
    public void setSessionMode(InterviewMode sessionMode) { this.sessionMode = sessionMode; }
    public InterviewStatus getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(InterviewStatus sessionStatus) { this.sessionStatus = sessionStatus; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public String getGeneralFeedback() { return generalFeedback; }
    public void setGeneralFeedback(String generalFeedback) { this.generalFeedback = generalFeedback; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
