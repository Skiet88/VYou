package za.ac.skiet.vyou.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSummary {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id")
    private InterviewSession interviewSession;

    private Integer overallScore;

    @Lob
    private String generalFeedback;

    @Lob
    private String recommendations;

    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "InterviewSummary{" +
                "id=" + id +
                ", interviewSession=" + interviewSession +
                ", overallScore=" + overallScore +
                ", generalFeedback='" + generalFeedback + '\'' +
                ", recommendations='" + recommendations + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

