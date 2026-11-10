package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewApiTest {

    @TestConfiguration
    static class FakeLlm {
        @Bean
        @Primary
        LlmClient fakeLlmClient() {
            AtomicInteger questions = new AtomicInteger();
            return (messages, jsonMode) -> jsonMode
                    ? """
                      {"clarity": 8, "technical": 7, "confidence": 6, "overall": 7,
                       "strengths": ["Concrete example"], "improvements": ["Quantify the impact"]}"""
                    : "Question " + questions.incrementAndGet() + "?";
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void fullInterviewFlow() throws Exception {
        String created = mvc.perform(post("/api/interviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"Java Developer\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.mode").value("TEXT"))
                .andReturn().getResponse().getContentAsString();
        JsonNode session = mapper.readTree(created);
        String id = session.get("id").asText();
        String firstQuestion = session.get("currentQuestion").asText();

        mvc.perform(post("/api/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"I used Spring Boot to build a REST API for a student portal.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn.sequenceNumber").value(1))
                .andExpect(jsonPath("$.turn.question").value(firstQuestion))
                .andExpect(jsonPath("$.turn.evaluation.overall").value(7))
                .andExpect(jsonPath("$.turn.evaluation.improvements[0]").value("Quantify the impact"))
                .andExpect(jsonPath("$.nextQuestion").exists());

        mvc.perform(post("/api/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"Not sure\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons[0]").exists());

        mvc.perform(post("/api/interviews/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/interviews/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(1));

        mvc.perform(post("/api/interviews/{id}/answers", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"Another answer that is long enough to pass.\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSessionIs404() throws Exception {
        mvc.perform(get("/api/interviews/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestIs400() throws Exception {
        mvc.perform(post("/api/interviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isBadRequest());
    }
}
