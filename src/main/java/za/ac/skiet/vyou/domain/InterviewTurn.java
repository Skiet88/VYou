package za.ac.skiet.vyou.domain;


import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "interview_turns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewTurn {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id")
    private InterviewSession interviewSession;

    private Integer sequenceNumber;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "question_content_id")
    private Content questionContent;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "answer_content_id")
    private Content answerContent;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ai_evaluation_id")
    private AiEvaluation aiEvaluation;

    @Override
    public String toString() {
        return "InterviewTurn{" +
                "id=" + id +
                ", interviewSession=" + interviewSession +
                ", sequenceNumber=" + sequenceNumber +
                ", questionContent=" + questionContent +
                ", answerContent=" + answerContent +
                ", aiEvaluation=" + aiEvaluation +
                '}';
    }
}
