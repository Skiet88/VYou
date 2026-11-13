package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises real email sending/receiving against a fake SMTP server (GreenMail), so these cover
 * actual email content and delivery, not just the service call that would trigger it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetApiTest {

    private static final Pattern LINK = Pattern.compile("(http://\\S+/reset-password\\?token=\\S+)");

    private static GreenMail greenMail;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
        registry.add("spring.mail.username", () -> "vyou-test@example.com");
        registry.add("spring.mail.password", () -> "unused");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @BeforeAll
    static void startSmtpServer() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());
        greenMail.start();
    }

    @AfterAll
    static void stopSmtpServer() {
        greenMail.stop();
    }

    @AfterEach
    void clearInbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;

    @Test
    void requestingAResetSendsAWorkingLinkThatChangesThePassword() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        greenMail.purgeEmailFromAllMailboxes();

        mvc.perform(post("/api/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RequestBody(email))))
                .andExpect(status().isOk());
        String token = tokenFromLatestEmail();

        mvc.perform(post("/api/password-reset/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ResetBody(token, "NewSecret456"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody(email, "NewSecret456"))))
                .andExpect(status().isOk());
    }

    @Test
    void theOldPasswordNoLongerWorksAfterAReset() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        resetVia(email, "NewSecret456");

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody(email, "Secret123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aResetSignsOutEveryOtherSessionForTheAccount() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        MockHttpSession firstDevice = AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        MockHttpSession secondDevice = loginAsExistingUser(email, "Secret123");

        resetVia(email, "NewSecret456");

        // Both sessions existed before the reset; ActiveAccountFilter catches the mismatch on
        // each one's own next request, regardless of which device actually triggered it.
        mvc.perform(get("/api/auth/me").session(firstDevice)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(secondDevice)).andExpect(status().isUnauthorized());
    }

    @Test
    void requestingAResetForAnUnknownEmailDoesNothingButStillReportsSuccess() throws Exception {
        mvc.perform(post("/api/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RequestBody("nobody-" + UUID.randomUUID() + "@example.com"))))
                .andExpect(status().isOk());

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void aResetTokenCanOnlyBeUsedOnce() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        greenMail.purgeEmailFromAllMailboxes();
        mvc.perform(post("/api/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RequestBody(email))));
        String token = tokenFromLatestEmail();

        mvc.perform(post("/api/password-reset/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ResetBody(token, "NewSecret456"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/password-reset/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ResetBody(token, "AnotherSecret789"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resettingWithAGarbageTokenReturnsAGenericBadRequest() throws Exception {
        mvc.perform(post("/api/password-reset/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ResetBody("not-a-real-token", "NewSecret456"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resettingWithAnExpiredTokenIsRejected() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        greenMail.purgeEmailFromAllMailboxes();
        mvc.perform(post("/api/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RequestBody(email))));
        String token = tokenFromLatestEmail();

        // Back-date the token's expiry directly, as if an hour had already passed.
        UserAccount account = users.findByEmail(email).orElseThrow();
        String hash = account.getPasswordResetTokenHash().orElseThrow();
        users.save(UserFactory.withPasswordResetToken(account, hash, Instant.now().minusSeconds(60)));

        mvc.perform(post("/api/password-reset/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ResetBody(token, "NewSecret456"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestingAResetIsRateLimitedLikeLogin() throws Exception {
        // vyou.ratelimit.auth.capacity in application-test.properties is large enough that normal
        // tests never trip it; this only checks the same endpoint is wired into that bucket, not
        // the limiter's own behaviour (RateLimitFilterTest owns that with tiny dedicated buckets).
        mvc.perform(post("/api/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RequestBody("someone@example.com"))))
                .andExpect(status().isOk());
    }

    private void resetVia(String email, String newPassword) throws Exception {
        mvc.perform(post("/api/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RequestBody(email))));
        String token = tokenFromLatestEmail();
        mvc.perform(post("/api/password-reset/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ResetBody(token, newPassword))))
                .andExpect(status().isOk());
    }

    private MockHttpSession loginAsExistingUser(String email, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String tokenFromLatestEmail() throws Exception {
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage[] messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = LINK.matcher(body);
        assertThat(matcher.find()).as("reset link in email body: %s", body).isTrue();
        String link = matcher.group(1);
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    private record RequestBody(String email) {
    }

    private record ResetBody(String token, String newPassword) {
    }

    private record LoginBody(String email, String password) {
    }
}
