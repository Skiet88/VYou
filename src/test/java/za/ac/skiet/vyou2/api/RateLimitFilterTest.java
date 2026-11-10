package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tiny, dedicated buckets so the limit trips in a handful of requests. The auth bucket is keyed
 * by client IP (MockMvc gives every request the same remote address, so this exercises it
 * correctly); the AI bucket is keyed by authenticated user id.
 *
 * The RateLimitFilter's bucket map is in-memory, singleton state on the Spring context. Without
 * DirtiesContext, both test methods below would share that one context (Spring's test context
 * cache) and silently consume each other's buckets - e.g. the AI test's signups count against
 * the IP-keyed auth bucket too. Each method gets its own fresh context instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "vyou.ratelimit.auth.capacity=2",
        "vyou.ratelimit.auth.refill-period=1h",
        "vyou.ratelimit.ai.capacity=2",
        "vyou.ratelimit.ai.refill-period=1h"
})
class RateLimitFilterTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void authEndpointIsLimitedByClientIpRegardlessOfCredentials() throws Exception {
        // Two failed logins consume the bucket (capacity 2); the third, still within the same
        // window and from the same "IP", is rejected before credentials are even checked.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new LoginBody("nobody@example.com", "wrong"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody("nobody@example.com", "wrong"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void aiEndpointIsLimitedPerUserNotGlobally() throws Exception {
        MockHttpSession userA = AuthTestSupport.signup(mvc, mapper, "ratea-" + UUID.randomUUID() + "@example.com", "Secret123");
        MockHttpSession userB = AuthTestSupport.signup(mvc, mapper, "rateb-" + UUID.randomUUID() + "@example.com", "Secret123");

        // userA's signup already consumed one slot of userA's own bucket (signup logs the user in,
        // but signup itself is not an AI-metered endpoint; only /api/interviews is). So userA has
        // the full AI bucket (2) available here.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/interviews").with(csrf()).session(userA).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 429) {
                            throw new AssertionError("Did not expect 429 within the user's own quota, got " + status);
                        }
                    });
        }
        mvc.perform(post("/api/interviews").with(csrf()).session(userA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isTooManyRequests());

        // userB is unaffected by userA having exhausted their own bucket.
        mvc.perform(post("/api/interviews").with(csrf()).session(userB).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 429) {
                        throw new AssertionError("userB should not be limited by userA's usage, got " + status);
                    }
                });
    }

    private record LoginBody(String email, String password) {
    }
}
