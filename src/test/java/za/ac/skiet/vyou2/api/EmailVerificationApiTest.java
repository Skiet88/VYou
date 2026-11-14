package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import za.ac.skiet.vyou2.testsupport.FakeResendServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises real email sending against a fake Resend API (an embedded HTTP server), so these cover
 * actual email content, not just the service call that would trigger it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailVerificationApiTest {

    private static final Pattern LINK = Pattern.compile("(http://\\S+/verify-email\\?token=\\S+)");

    private static FakeResendServer resend;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("resend.base-url", () -> "http://127.0.0.1:" + resend.port());
        registry.add("resend.api-key", () -> "test-key");
        registry.add("vyou.mail-from", () -> "vyou-test@example.com");
    }

    @BeforeAll
    static void startFakeResend() {
        resend = new FakeResendServer();
    }

    @AfterAll
    static void stopFakeResend() {
        resend.close();
    }

    @AfterEach
    void clearInbox() {
        resend.clear();
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;

    @Test
    void signupSendsAWorkingVerificationLink() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");

        String token = tokenFromLatestEmail();

        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody(token))))
                .andExpect(status().isOk());

        assertThat(users.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void onceVerifiedTheAccountCanUseProtectedEndpoints() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@example.com";
        MockHttpSession session = AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        String token = tokenFromLatestEmail();

        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody(token))))
                .andExpect(status().isOk());

        // ActiveAccountFilter refreshes the session's cached principal from the DB on this next
        // request, so the now-verified account is let through without a fresh login. A read-only
        // endpoint is used here so the assertion isn't entangled with LLM availability.
        mvc.perform(get("/api/interviews/mine").session(session)).andExpect(status().isOk());
    }

    @Test
    void unverifiedAccountIsBlockedFromProtectedEndpointsButNotFromExemptOnes() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@example.com";
        MockHttpSession session = AuthTestSupport.signup(mvc, mapper, email, "Secret123");

        mvc.perform(post("/api/interviews").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobRole\":\"Dev\",\"level\":\"JUNIOR\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.properties.code").value("EMAIL_NOT_VERIFIED"));

        // /api/auth/** stays reachable even while unverified (needed to sign out, or re-check who
        // you are), and so does /api/email-verification/** (needed to act on the block itself).
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/email-verification/resend").with(csrf()).session(session))
                .andExpect(status().isOk());
    }

    @Test
    void resendIsANoOpOnceAlreadyVerified() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@example.com";
        MockHttpSession session = AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        String token = tokenFromLatestEmail();
        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody(token))))
                .andExpect(status().isOk());
        resend.clear();

        mvc.perform(post("/api/email-verification/resend").with(csrf()).session(session))
                .andExpect(status().isOk());

        assertThat(resend.count()).isZero();
    }

    @Test
    void resendSendsAFreshTokenThatSupersedesTheOldOne() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@example.com";
        MockHttpSession session = AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        String firstToken = tokenFromLatestEmail();
        resend.clear();

        mvc.perform(post("/api/email-verification/resend").with(csrf()).session(session))
                .andExpect(status().isOk());
        String secondToken = tokenFromLatestEmail();

        assertThat(secondToken).isNotEqualTo(firstToken);
        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody(firstToken))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody(secondToken))))
                .andExpect(status().isOk());
    }

    @Test
    void verifyingWithAGarbageTokenReturnsAGenericBadRequest() throws Exception {
        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody("not-a-real-token"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyingWithAnExpiredTokenIsRejected() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        String token = tokenFromLatestEmail();

        // Back-date the token's expiry directly, as if 24 hours had already passed.
        UserAccount account = users.findByEmail(email).orElseThrow();
        String hash = account.getVerificationTokenHash().orElseThrow();
        users.save(UserFactory.withVerificationToken(account, hash, Instant.now().minusSeconds(60)));

        mvc.perform(post("/api/email-verification/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new VerifyBody(token))))
                .andExpect(status().isBadRequest());
    }

    private String tokenFromLatestEmail() {
        String body = resend.lastEmailText();
        Matcher matcher = LINK.matcher(body);
        assertThat(matcher.find()).as("verification link in email body: %s", body).isTrue();
        String link = matcher.group(1);
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    private record VerifyBody(String token) {
    }
}
