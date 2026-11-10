package za.ac.skiet.vyou2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vyou.interview")
public record InterviewProperties(Integer questionCount) {

    public InterviewProperties {
        questionCount = questionCount == null ? 5 : questionCount;
    }
}
