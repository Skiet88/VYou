package za.ac.skiet.vyou.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import za.ac.skiet.vyou.ENUM.InterviewMode;
import za.ac.skiet.vyou.ENUM.InterviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserEntity user;

    @Column(nullable = false)
    private String jobRole;

    @Column(nullable = false)
    private String level;

    @Enumerated(EnumType.STRING)
    private InterviewMode mode;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Override
    public String toString() {
        return "InterviewSession{" +
                "id=" + id +
//                ", user=" + user.getEmail() +
                ", jobRole='" + jobRole + '\'' +
                ", level='" + level + '\'' +
                ", mode=" + mode +
                ", status=" + status +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                '}';
    }
}

