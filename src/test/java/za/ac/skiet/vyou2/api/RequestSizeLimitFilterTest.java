package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A tiny limit, just for this test class, so an oversized body is cheap and fast to construct. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "vyou.security.max-request-bytes=200")
class RequestSizeLimitFilterTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void oversizedBodyIsRejectedBeforeReachingTheController() throws Exception {
        MockHttpSession session = AuthTestSupport.signup(mvc, mapper, "size-" + UUID.randomUUID() + "@example.com", "Secret123");
        String tooLong = "A".repeat(1000);

        mvc.perform(post("/api/interviews").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("jobRole", tooLong, "level", "JUNIOR"))))
                .andExpect(status().isPayloadTooLarge());
    }
}
