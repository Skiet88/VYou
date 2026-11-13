package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.exception.LlmUnavailableException;
import za.ac.skiet.vyou2.llm.ChatMessage;
import za.ac.skiet.vyou2.llm.LlmClient;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @Autowired
    private UserRepository users;

    @BeforeEach
    void llmUp() {
        llm.down.set(false);
    }

    @Test
    void fullInterviewFlowWithInstantFeedback() throws Exception {
        MockHttpSession session = newUser();
        JsonNode started = start(session, "{\"jobRole\":\"Java Developer\",\"level\":\"JUNIOR\"}");
        String id = started.get("id").asText();
        String firstQuestion = started.get("currentQuestion").asText();
        assertEquals(started, "feedbackMode", "INSTANT");
        assertEquals(started, "totalQuestions", 5);

        answer(session, id, GOOD_ANSWER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn.sequenceNumber").value(1))
                .andExpect(jsonPath("$.turn.question").value(firstQuestion))
                .andExpect(jsonPath("$.turn.evaluation.overall").value(7))
                .andExpect(jsonPath("$.turn.evaluation.improvements[0]").value("Quantify the impact"))
                .andExpect(jsonPath("$.nextQuestion").exists());

        answer(session, id, "Not sure")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons[0]").exists());

        mvc.perform(post("/api/interviews/{id}/complete", id).with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("COMPLETED"))
                .andExpect(jsonPath("$.summary.feedback").value("Solid start."))
                .andExpect(jsonPath("$.summary.averageOverall").value(7.0));

        mvc.perform(get("/api/interviews/{id}", id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(1))
                .andExpect(jsonPath("$.turns[0].evaluation.overall").value(7))
                .andExpect(jsonPath("$.summary.focusAreas[0]").value("Depth"));

        mvc.perform(get("/api/interviews/mine").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalItems").value(1));

        answer(session, id, "Another answer that is long enough to pass.").andExpect(status().isConflict());
        mvc.perform(post("/api/interviews/{id}/complete", id).with(csrf()).session(session)).andExpect(status().isConflict());
    }

    @Test
    void endOfInterviewModeHidesScoresUntilCompletedThenAutoCompletesAtQuestionLimit() throws Exception {
        MockHttpSession session = newUser();
        JsonNode started = start(session, "{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\",\"feedbackMode\":\"END_OF_INTERVIEW\"}");
        String id = started.get("id").asText();

        for (int i = 1; i <= 4; i++) {
            answer(session, id, GOOD_ANSWER + " attempt " + i)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.turn.evaluation").doesNotExist())
                    .andExpect(jsonPath("$.sessionStatus").value("IN_PROGRESS"));
        }
        // 5th (last, totalQuestions=5) answer auto-completes the session; no next question needed.
        answer(session, id, GOOD_ANSWER + " attempt 5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextQuestion").doesNotExist())
                .andExpect(jsonPath("$.sessionStatus").value("COMPLETED"));

        mvc.perform(get("/api/interviews/{id}", id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(5))
                .andExpect(jsonPath("$.turns[0].evaluation.overall").value(7));
    }

    @Test
    void userCannotReachAnotherUsersInterview() throws Exception {
        MockHttpSession owner = newUser();
        MockHttpSession intruder = newUser();
        String id = start(owner, "{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}").get("id").asText();

        mvc.perform(get("/api/interviews/{id}", id).session(intruder)).andExpect(status().isNotFound());
        answer(intruder, id, GOOD_ANSWER).andExpect(status().isNotFound());
        mvc.perform(post("/api/interviews/{id}/complete", id).with(csrf()).session(intruder)).andExpect(status().isNotFound());
        mvc.perform(post("/api/interviews/{id}/summary", id).with(csrf()).session(intruder)).andExpect(status().isNotFound());

        // The owner can still reach it normally.
        mvc.perform(get("/api/interviews/{id}", id).session(owner)).andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/interviews/mine")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/interviews").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void maliciousLookingAnswersAreStoredAsPlainTextNotExecuted() throws Exception {
        MockHttpSession session = newUser();
        String id = start(session, "{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}").get("id").asText();
        String xss = "<script>alert('xss')</script> this is my real answer about REST APIs and databases";
        String sqli = "'); DROP TABLE users; -- this is my real answer about testing practices";

        answer(session, id, xss).andExpect(status().isOk())
                .andExpect(jsonPath("$.turn.answer").value(xss));

        // Ends the interview early (a valid action, independent of the question limit).
        mvc.perform(post("/api/interviews/{id}/complete", id).with(csrf()).session(session))
                .andExpect(status().isOk());

        // The SQL-shaped text must not have damaged anything: the session is already completed,
        // so this is correctly rejected as a conflict rather than crashing.
        answer(session, id, sqli).andExpect(status().isConflict());

        // The database and this session are still intact and queryable.
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/interviews/{id}", id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns[0].answer").value(xss));
    }

    @Test
    void frontendOriginIsAllowedByCors() throws Exception {
        mvc.perform(options("/api/interviews")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void openApiSpecIsReachableInTestProfile() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/interviews/{id}/answers']").exists());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    private MockHttpSession newUser() throws Exception {
        return AuthTestSupport.signupVerified(mvc, mapper, users, "user-" + UUID.randomUUID() + "@example.com", "Secret123");
    }

    private JsonNode start(MockHttpSession session, String body) throws Exception {
        String response = mvc.perform(post("/api/interviews").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    private ResultActions answer(MockHttpSession session, String id, String answerText) throws Exception {
        return mvc.perform(post("/api/interviews/{id}/answers", id).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("answer", answerText))));
    }

    private static void assertEquals(JsonNode node, String field, Object expected) {
        org.assertj.core.api.Assertions.assertThat(node.get(field).asText()).isEqualTo(String.valueOf(expected));
    }
}
