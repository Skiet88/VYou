package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.config.LlmProperties;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import za.ac.skiet.vyou2.exception.LlmUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** POSTs to {base-url}/chat/completions using the OpenAI wire format. */
public class OpenAiCompatibleLlmClient implements LlmClient {

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

        JsonNode response;
        try {
            response = restClient.post()
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
            throw new LlmUnavailableException("Provider returned HTTP " + status.value(), ex);
        } catch (ResourceAccessException ex) {
            throw new LlmUnavailableException("Could not reach " + properties.baseUrl(), ex);
        }

        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || content.isMissingNode() || content.asText().isBlank()) {
            throw new LlmResponseException("Response had no message content");
        }
        return content.asText();
    }
}
