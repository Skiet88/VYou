package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.factory.EvaluationFactory;
import za.ac.skiet.vyou2.factory.InterviewSessionFactory;
import za.ac.skiet.vyou2.factory.InterviewSummaryFactory;
import za.ac.skiet.vyou2.factory.InterviewTurnFactory;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import za.ac.skiet.vyou2.repository.InterviewTurnRepository;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
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
class AdminUserApiTest {

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

    private record Account(MockHttpSession session, UUID id, String email) {
    }

    private Account signup(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        MockHttpSession session = AuthTestSupport.signup(mvc, mapper, email, "Secret123");
        UUID id = users.findByEmail(email).orElseThrow().getId();
        return new Account(session, id, email);
    }

    private Account admin() throws Exception {
        Account account = signup("admin");
        setRole(account.id(), UserRole.ADMIN);
        return account;
    }

    private void setRole(UUID id, UserRole role) {
        users.save(UserFactory.withRole(users.findById(id).orElseThrow(), role));
    }

    private JsonNode list(Account admin, String query) throws Exception {
        String body = mvc.perform(get("/api/admin/users" + query).session(admin.session()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private JsonNode find(JsonNode page, UUID id) {
        for (JsonNode item : page.get("items")) {
            if (item.get("id").asText().equals(id.toString())) return item;
        }
        throw new AssertionError("user " + id + " not in page");
    }

    @Test
    void onlyAdminsCanUseTheAdminApi() throws Exception {
        Account user = signup("plain");
        mvc.perform(get("/api/admin/users").session(user.session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/users/{id}/suspend", user.id()).with(csrf()).session(user.session()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsAccountsWithAggregateNumbersButNoInterviewContent() throws Exception {
        Account admin = admin();
        Account candidate = signup("candidate");
        var session = sessions.save(InterviewSessionFactory.start(candidate.id(), FeedbackMode.INSTANT, 5, "Secret Project Role",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q1", Instant.now()));
        var turn = turns.save(InterviewTurnFactory.create(session.getId(), 1, "Q1", "My confidential answer",
                EvaluationFactory.create(6, 7, 8, 7, List.of("Good"), List.of("Better")), Instant.now()));
        sessions.save(InterviewSessionFactory.complete(sessions.findById(session.getId()).orElseThrow(), Instant.now()));
        summaries.save(InterviewSummaryFactory.create(session.getId(), List.of(turn), "Private feedback", List.of("A"), List.of("B"), Instant.now()));
        sessions.save(InterviewSessionFactory.start(candidate.id(), FeedbackMode.INSTANT, 5, "Other", ExperienceLevel.MID,
                InterviewMode.TEXT, "Q1", Instant.now()));

        JsonNode page = list(admin, "?search=" + candidate.email());
        JsonNode row = find(page, candidate.id());
        assertThat(row.get("interviewsTotal").asInt()).isEqualTo(2);
        assertThat(row.get("interviewsCompleted").asInt()).isEqualTo(1);
        assertThat(row.get("averageOverall").asDouble()).isEqualTo(7.0);
        assertThat(row.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(page.toString())
                .doesNotContain("My confidential answer")
                .doesNotContain("Private feedback")
                .doesNotContain("Secret Project Role")
                .doesNotContain("password");
    }

    @Test
    void anAdminStillCannotOpenSomeoneElsesInterview() throws Exception {
        Account admin = admin();
        Account candidate = signup("candidate");
        var session = sessions.save(InterviewSessionFactory.start(candidate.id(), FeedbackMode.INSTANT, 5, "Role",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q1", Instant.now()));
        mvc.perform(get("/api/interviews/{id}", session.getId()).session(admin.session())).andExpect(status().isNotFound());
    }

    @Test
    void searchMatchesNameOrEmailAndTreatsWildcardsLiterally() throws Exception {
        Account admin = admin();
        String marker = "zq" + UUID.randomUUID().toString().substring(0, 6);
        Account a = signup(marker + "-one");
        Account b = signup(marker + "-two");
        signup("unrelated");

        JsonNode byEmail = list(admin, "?search=" + marker.toUpperCase());
        assertThat(byEmail.get("totalItems").asLong()).isEqualTo(2);
        find(byEmail, a.id());
        find(byEmail, b.id());

        // "%" and "_" are ordinary characters, not wildcards that would match every user.
        assertThat(list(admin, "?search=%25").get("totalItems").asLong()).isZero();
        assertThat(list(admin, "?search=_").get("totalItems").asLong()).isZero();

        mvc.perform(get("/api/admin/users?search=" + "x".repeat(101)).session(admin.session())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users?size=51").session(admin.session())).andExpect(status().isBadRequest());
    }

    @Test
    void suspensionTakesEffectOnTheUsersNextRequestAndReinstatingRestoresAccess() throws Exception {
        Account admin = admin();
        Account target = signup("target");
        mvc.perform(get("/api/auth/me").session(target.session())).andExpect(status().isOk());

        mvc.perform(post("/api/admin/users/{id}/suspend", target.id()).with(csrf()).session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Their existing, previously valid session stops working immediately.
        mvc.perform(get("/api/auth/me").session(target.session())).andExpect(status().isUnauthorized());
        login(target.email()).andExpect(status().isUnauthorized());
        assertThat(list(admin, "?status=SUSPENDED&search=" + target.email()).get("totalItems").asLong()).isEqualTo(1);

        mvc.perform(post("/api/admin/users/{id}/reinstate", target.id()).with(csrf()).session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        login(target.email()).andExpect(status().isOk());
    }

    @Test
    void adminsCannotSuspendThemselvesOrOtherAdmins() throws Exception {
        Account admin = admin();
        Account otherAdmin = admin();
        mvc.perform(post("/api/admin/users/{id}/suspend", admin.id()).with(csrf()).session(admin.session()))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/users/{id}/suspend", otherAdmin.id()).with(csrf()).session(admin.session()))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/users/{id}/suspend", UUID.randomUUID()).with(csrf()).session(admin.session()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/users/{id}/suspend", admin.id()).session(admin.session()))
                .andExpect(status().isForbidden()); // no CSRF token
    }

    @Test
    void roleChangesInTheDatabaseApplyWithoutLoggingInAgain() throws Exception {
        Account user = signup("promoted");
        mvc.perform(get("/api/admin/users").session(user.session())).andExpect(status().isForbidden());

        setRole(user.id(), UserRole.ADMIN);
        mvc.perform(get("/api/admin/users").session(user.session())).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(user.session())).andExpect(jsonPath("$.role").value("ADMIN"));

        setRole(user.id(), UserRole.USER);
        mvc.perform(get("/api/admin/users").session(user.session())).andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "password", "Secret123"))));
    }
}
