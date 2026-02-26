package za.ac.skiet.vyou.domain;



import jakarta.persistence.*;
import lombok.*;
import za.ac.skiet.vyou.ENUM.ContentType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Lob
    private String textData;

    private String mediaUrl;

    private Integer duration; // seconds

    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "Content{" +
                "id=" + id +
                ", contentType=" + contentType +
                ", textData='" + textData + '\'' +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", duration=" + duration +
                ", createdAt=" + createdAt +
                '}';
    }
}

