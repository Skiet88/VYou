package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.exception.LlmUnavailableException;
import za.ac.skiet.vyou2.llm.ChatMessage;
import za.ac.skiet.vyou2.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InterviewApiTest {

    private static final String GOOD_ANSWER = "I used Spring Boot to build a REST API for a student portal.";

    /** Fake GPT-4o: canned evaluation/summary JSON, numbered questions, and a switchable outage. */
    static class FakeLlm implements LlmClient {
        final AtomicBoolean down = new AtomicBoolean(false);
        final AtomicInteger questions = new AtomicInteger();

        @Override
        public String complete(List<ChatMessage> messages, boolean jsonMode) {
            if (down.get()) {
                throw new LlmUnavailableException("Rate limit reached");
            }
            if (!jsonMode) {
                return "Question " + questions.incrementAndGet() + "?";
            }
            if (messages.get(0).content().contains("debrief")) {
                return "{\"feedback\": \"Solid start.\", \"topStrengths\": [\"Examples\"], \"focusAreas\": [\"Depth\"]}";
            }
            return """
                    {"clarity": 8, "technical": 7, "confidence": 6, "overall": 7,
                     "strengths": ["Concrete example"], "improvements": ["Quantify the impact"]}""";
        }
    }

    @TestConfiguration
    static class FakeLlmConfig {
        @Bean
        @Primary
        FakeLlm fakeLlmClient() {
            return new FakeLlm();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private FakeLlm llm;

    @BeforeEach
    void llmUp() {
        llm.down.set(false);
    }

    @Test
    void fullInterviewFlow() throws Exception {
        String userId = registerUser();
        JsonNode session = startInterview(userId);
        String id = session.get("id").asText();
        String firstQuestion = session.get("currentQuestion").asText();

        answer(id, GOOD_ANSWER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn.sequenceNumber").value(1))
                .andExpect(jsonPath("$.turn.question").value(firstQuestion))
                .andExpect(jsonPath("$.turn.evaluation.overall").value(7))
                .andExpect(jsonPath("$.turn.evaluation.improvements[0]").value("Quantify the impact"))
                .andExpect(jsonPath("$.nextQuestion").exists());

        answer(id, "Not sure")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons[0]").exists());

        mvc.perform(post("/api/interviews/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"))
                .andExpect(jsonPath("$.summary.feedback").value("Solid start."))
                .andExpect(jsonPath("$.summary.averageOverall").value(7.0));

        mvc.perform(get("/api/interviews/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(1))
                .andExpect(jsonPath("$.summary.focusAreas[0]").value("Depth"));

        mvc.perform(get("/api/users/{id}/interviews", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        answer(id, "Another answer that is long enough to pass.").andExpect(status().isConflict());
        mvc.perform(post("/api/interviews/{id}/complete", id)).andExpect(status().isConflict());
    }

    @Test
    void llmOutageNeverLosesWork() throws Exception {
        String id = startInterview(registerUser()).get("id").asText();
        answer(id, GOOD_ANSWER).andExpect(status().isOk());

        llm.down.set(true);
        answer(id, GOOD_ANSWER)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Rate limit reached"));

        // Completion still succeeds; the summary is missing and can be retried later.
        mvc.perform(post("/api/interviews/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"))
                .andExpect(jsonPath("$.summary").doesNotExist());

        llm.down.set(false);
        mvc.perform(post("/api/interviews/{id}/summary", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionsAnswered").value(1));
        mvc.perform(get("/api/interviews/{id}", id))
                .andExpect(jsonPath("$.summary.feedback").value("Solid start."));
    }

    @Test
    void summaryBeforeCompletionIsConflict() throws Exception {
        String id = startInterview(registerUser()).get("id").asText();
        mvc.perform(post("/api/interviews/{id}/summary", id)).andExpect(status().isConflict());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        String body = "{\"email\":\"" + email + "\",\"displayName\":\"A\"}";
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toUpperCase()
                        .replace("EMAIL", "email").replace("DISPLAYNAME", "displayName")))
                .andExpect(status().isConflict());
    }

    @Test
    void startForUnknownUserIs404() throws Exception {
        mvc.perform(post("/api/interviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + UUID.randomUUID() + "\",\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void badInputIs400() throws Exception {
        mvc.perform(post("/api/interviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/interviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + UUID.randomUUID() + "\",\"jobRole\":\"Dev\",\"level\":\"GURU\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/interviews/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/interviews/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void frontendOriginIsAllowed() throws Exception {
        mvc.perform(options("/api/interviews")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void openApiSpecIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/interviews/{id}/answers']").exists());
    }

    private String registerUser() throws Exception {
        String body = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user-" + UUID.randomUUID() + "@example.com\",\"displayName\":\"Lerato\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asText();
    }

    private JsonNode startInterview(String userId) throws Exception {
        String body = mvc.perform(post("/api/interviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"jobRole\":\"Java Developer\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.mode").value("TEXT"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private ResultActions answer(String id, String answer) throws Exception {
        return mvc.perform(post("/api/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("answer", answer))));
    }
}
