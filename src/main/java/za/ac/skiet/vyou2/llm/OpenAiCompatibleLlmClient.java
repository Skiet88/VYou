package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.config.LlmProperties;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import za.ac.skiet.vyou2.exception.LlmUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** POSTs to {base-url}/chat/completions using the OpenAI wire format. */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final RestClient restClient;
    private final LlmProperties properties;

    public OpenAiCompatibleLlmClient(RestClient restClient, LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String complete(List<ChatMessage> messages, boolean jsonMode) {
        if (!properties.isConfigured()) {
            throw new LlmUnavailableException(
                    "No API key configured. Set GITHUB_TOKEN (GitHub Models) or VYOU_LLM_API_KEY.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("messages", messages);
        body.put("temperature", properties.temperature());
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        JsonNode response = sendWithRetry(body);

        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || content.isMissingNode() || content.asText().isBlank()) {
            throw new LlmResponseException("Response had no message content");
        }
        return content.asText();
    }

    /**
     * Retries network failures and 5xx responses with a short backoff. 4xx responses, including
     * 429 rate limits, are not retried: retrying a daily quota limit only wastes requests.
     */
    private JsonNode sendWithRetry(Map<String, Object> body) {
        int attempts = Math.max(1, properties.maxAttempts());
        for (int attempt = 1; ; attempt++) {
            try {
                return restClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException ex) {
                HttpStatusCode status = ex.getStatusCode();
                if (status.value() == 429) {
                    throw new LlmUnavailableException("Rate limit reached for " + properties.model()
                            + "; the free quota resets daily", ex);
                }
                if (status.is4xxClientError() || attempt >= attempts) {
                    throw new LlmUnavailableException("Provider returned HTTP " + status.value(), ex);
                }
                log.warn("LLM call failed with HTTP {} (attempt {}/{}), retrying", status.value(), attempt, attempts);
            } catch (ResourceAccessException ex) {
                if (attempt >= attempts) {
                    throw new LlmUnavailableException("Could not reach " + properties.baseUrl(), ex);
                }
                log.warn("LLM call failed: {} (attempt {}/{}), retrying", ex.getMessage(), attempt, attempts);
            }
            sleep(properties.retryBackoff().toMillis() * attempt);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("Interrupted while waiting to retry");
        }
    }
}
