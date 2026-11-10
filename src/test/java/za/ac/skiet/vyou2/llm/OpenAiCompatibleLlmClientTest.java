package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.config.LlmProperties;
import za.ac.skiet.vyou2.exception.LlmUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleLlmClientTest {

    private static final String URL = "https://models.github.ai/inference/chat/completions";
    private static final String OK = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"}}]}";

    private final LlmProperties props = new LlmProperties(null, null, "test-key", null, null, 2, Duration.ofMillis(1));
    private final RestClient.Builder builder = RestClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader("Authorization", "Bearer " + props.apiKey());
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final LlmClient client = new OpenAiCompatibleLlmClient(builder.build(), props);

    @Test
    void sendsGpt4oRequestWithJsonModeAndAuth() {
        server.expect(once(), requestTo(URL))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("openai/gpt-4o"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        assertThat(client.complete(List.of(ChatMessage.user("hi")), true)).isEqualTo("Hello");
        server.verify();
    }

    @Test
    void retriesServerErrorOnce() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(once(), requestTo(URL)).andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        assertThat(client.complete(List.of(ChatMessage.user("hi")), false)).isEqualTo("Hello");
        server.verify();
    }

    @Test
    void doesNotRetryRateLimit() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.complete(List.of(ChatMessage.user("hi")), false))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("Rate limit");
        server.verify();
    }

    @Test
    void missingKeyFailsWithoutCallingProvider() {
        LlmProperties noKey = new LlmProperties(null, null, "", null, null, null, null);
        LlmClient unconfigured = new OpenAiCompatibleLlmClient(RestClient.create(), noKey);
        assertThatThrownBy(() -> unconfigured.complete(List.of(ChatMessage.user("hi")), false))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("GITHUB_TOKEN");
    }
}
