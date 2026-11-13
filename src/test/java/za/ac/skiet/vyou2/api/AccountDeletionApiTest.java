package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;
import za.ac.skiet.vyou2.factory.InterviewSessionFactory;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import za.ac.skiet.vyou2.repository.InterviewTurnRepository;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.service.AccountDeletionService;
import za.ac.skiet.vyou2.service.AccountPurgeScheduler;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeletionApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private InterviewSessionRepository sessions;
    @Autowired
    private InterviewTurnRepository turns;
    @Autowired
    private InterviewSummaryRepository summaries;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Clock clock;
    @Autowired
    private AccountDeletionService accountDeletionService;
    @Autowired
    private AccountPurgeScheduler purgeScheduler;

    private record Account(MockHttpSession session, UUID id, String email) {
    }

    private Account signup(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        MockHttpSession session = AuthTestSupport.signupVerified(mvc, mapper, users, email, "Secret123");
        UUID id = users.findByEmail(email).orElseThrow().getId();
        return new Account(session, id, email);
    }

    private void requestDeletion(Account account, String password) throws Exception {
        mvc.perform(post("/api/account/delete").with(csrf()).session(account.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("password", password))))
                .andExpect(status().isOk());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void wrongPasswordIsRejectedAndNothingIsScheduled() throws Exception {
        Account account = signup("delete");
        mvc.perform(post("/api/account/delete").with(csrf()).session(account.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("password", "WrongPassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Incorrect password"));
        assertThat(users.findById(account.id()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void requestingDeletionSignsOutImmediatelyAndSchedulesAPurgeDate() throws Exception {
        Account account = signup("delete");
        mvc.perform(get("/api/auth/me").session(account.session())).andExpect(status().isOk());

        requestDeletion(account, "Secret123");

        mvc.perform(get("/api/auth/me").session(account.session())).andExpect(status().isUnauthorized());
        var stored = users.findById(account.id()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(UserStatus.PENDING_DELETION);
        assertThat(stored.getDeletionScheduledAt()).hasValueSatisfying(
                purgeAt -> assertThat(purgeAt).isAfter(clock.instant().plusSeconds(29 * 86400)));
    }

    @Test
    void anUnauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(post("/api/account/delete").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("password", "whatever"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestWithoutCsrfIsRejected() throws Exception {
        Account account = signup("delete");
        mvc.perform(post("/api/account/delete").session(account.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("password", "Secret123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theOnlyAdminCannotDeleteTheirOwnAccount() throws Exception {
        Account admin = signup("onlyadmin");
        users.save(UserFactory.withRole(users.findById(admin.id()).orElseThrow(), UserRole.ADMIN));
        mvc.perform(post("/api/account/delete").with(csrf()).session(admin.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("password", "Secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("You're the only admin. Promote another admin before deleting your account."));
        assertThat(users.findById(admin.id()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void anAdminCanDeleteTheirOwnAccountWhenAnotherAdminExists() throws Exception {
        Account first = signup("admin1");
        Account second = signup("admin2");
        users.save(UserFactory.withRole(users.findById(first.id()).orElseThrow(), UserRole.ADMIN));
        users.save(UserFactory.withRole(users.findById(second.id()).orElseThrow(), UserRole.ADMIN));

        requestDeletion(first, "Secret123");
        assertThat(users.findById(first.id()).orElseThrow().getStatus()).isEqualTo(UserStatus.PENDING_DELETION);
    }

    @Test
    void signingInAgainCancelsTheScheduledDeletionAndReportsIt() throws Exception {
        Account account = signup("cancel");
        requestDeletion(account, "Secret123");

        JsonNode wrongPassword = login(account.email(), "WrongPassword1");
        assertThat(wrongPassword.get("detail").asText()).isEqualTo("Invalid email or password");
        assertThat(users.findById(account.id()).orElseThrow().getStatus()).isEqualTo(UserStatus.PENDING_DELETION);

        JsonNode response = login(account.email(), "Secret123");
        assertThat(response.get("accountReactivated").asBoolean()).isTrue();
        assertThat(response.get("user").get("email").asText()).isEqualTo(account.email());
        var restored = users.findById(account.id()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(restored.getDeletionScheduledAt()).isEmpty();
    }

    @Test
    void anOrdinaryLoginReportsNoReactivation() throws Exception {
        Account account = signup("plain");
        JsonNode response = login(account.email(), "Secret123");
        assertThat(response.get("accountReactivated").asBoolean()).isFalse();
    }

    @Test
    void anAdminCannotSuspendOrReinstateAPendingDeletionAccount() throws Exception {
        Account admin = signup("admin");
        users.save(UserFactory.withRole(users.findById(admin.id()).orElseThrow(), UserRole.ADMIN));
        Account target = signup("target");
        requestDeletion(target, "Secret123");

        mvc.perform(post("/api/admin/users/{id}/suspend", target.id()).with(csrf()).session(admin.session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This account is scheduled for deletion and can't be suspended"));
        mvc.perform(post("/api/admin/users/{id}/reinstate", target.id()).with(csrf()).session(admin.session()))
                .andExpect(status().isConflict());
    }

    @Test
    void adminListingShowsPendingDeletionAccountsWhenFilteredByStatus() throws Exception {
        Account admin = signup("admin");
        users.save(UserFactory.withRole(users.findById(admin.id()).orElseThrow(), UserRole.ADMIN));
        Account target = signup("target");
        requestDeletion(target, "Secret123");

        String body = mvc.perform(get("/api/admin/users?status=PENDING_DELETION&search=" + target.email())
                        .session(admin.session()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode page = mapper.readTree(body);
        assertThat(page.get("totalItems").asLong()).isEqualTo(1);
        assertThat(page.get("items").get(0).get("status").asText()).isEqualTo("PENDING_DELETION");
    }

    @Test
    void purgeJobDeletesAnAccountAndEverythingItOwnsOnceDue() throws Exception {
        Account account = signup("purge");
        // Seeded directly (no AI call needed) rather than through POST /api/interviews.
        var session = sessions.save(InterviewSessionFactory.start(account.id(), FeedbackMode.INSTANT, 5, "Dev",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q1", clock.instant()));
        UUID sessionId = session.getId();
        requestDeletion(account, "Secret123");

        // Not due yet: the scheduler leaves it alone.
        purgeScheduler.purgeDueAccounts();
        assertThat(users.findById(account.id())).isPresent();
        assertThat(sessions.findById(sessionId)).isPresent();

        // Back-date the purge time directly, as if the 30 days had already passed.
        var pending = users.findById(account.id()).orElseThrow();
        users.save(new za.ac.skiet.vyou2.domain.UserAccount(pending.getId(), pending.getEmail(), pending.getDisplayName(),
                pending.getPasswordHash(), pending.getRole(), UserStatus.PENDING_DELETION, pending.getCreatedAt(),
                clock.instant().minusSeconds(60), pending.isEmailVerified(),
                pending.getVerificationTokenHash().orElse(null), pending.getVerificationTokenExpiresAt().orElse(null),
                pending.getPasswordChangedAt(), pending.getPasswordResetTokenHash().orElse(null),
                pending.getPasswordResetTokenExpiresAt().orElse(null)));

        purgeScheduler.purgeDueAccounts();

        assertThat(users.findById(account.id())).isEmpty();
        assertThat(sessions.findById(sessionId)).isEmpty();
        assertThat(turns.findBySessionIdOrderBySequenceNumberAsc(sessionId)).isEmpty();
    }

    @Test
    void purgeOneIsANoOpForAnAccountThatIsNoLongerPendingDeletion() throws Exception {
        Account account = signup("notpending");
        accountDeletionService.purgeOne(account.id());
        assertThat(users.findById(account.id())).isPresent();
    }
}
