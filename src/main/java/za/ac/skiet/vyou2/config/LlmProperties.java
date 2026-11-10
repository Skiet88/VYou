package za.ac.skiet.vyou2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Defaults to GPT-4o on GitHub Models (free, daily rate limit). Any OpenAI-compatible
 * provider works by changing base-url/model, e.g. Groq: https://api.groq.com/openai/v1.
 */
@ConfigurationProperties(prefix = "vyou.llm")
public record LlmProperties(String baseUrl, String model, String apiKey, Double temperature, Duration timeout,
                            Integer maxAttempts, Duration retryBackoff) {

    public LlmProperties {
        baseUrl = isBlank(baseUrl) ? "https://models.github.ai/inference" : baseUrl.replaceAll("/+$", "");
        model = isBlank(model) ? "openai/gpt-4o" : model;
        temperature = temperature == null ? 0.2 : temperature;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        maxAttempts = maxAttempts == null ? 2 : maxAttempts;
        retryBackoff = retryBackoff == null ? Duration.ofMillis(500) : retryBackoff;
    }

    public boolean isConfigured() {
        return !isBlank(apiKey);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
