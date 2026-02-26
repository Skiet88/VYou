package za.ac.skiet.vyou.domain;



import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ai_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiEvaluation {

    @Id
    @GeneratedValue
    private UUID id;

    private Integer confidenceScore;
    private Integer clarityScore;
    private Integer technicalScore;
    private Integer overallScore;

    @Lob
    private String strengths;

    @Lob
    private String improvements;

    @Override
    public String toString() {
        return "AiEvaluation{" +
                "id=" + id +
                ", confidenceScore=" + confidenceScore +
                ", clarityScore=" + clarityScore +
                ", technicalScore=" + technicalScore +
                ", overallScore=" + overallScore +
                ", strengths='" + strengths + '\'' +
                ", improvements='" + improvements + '\'' +
                '}';
    }
}
