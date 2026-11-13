package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.factory.EvaluationFactory;
import za.ac.skiet.vyou2.factory.InterviewSessionFactory;
import za.ac.skiet.vyou2.factory.InterviewSummaryFactory;
import za.ac.skiet.vyou2.factory.InterviewTurnFactory;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import za.ac.skiet.vyou2.repository.InterviewTurnRepository;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class InterviewListApiTest {

    private static final Instant BASE = Instant.parse("2026-01-01T09:00:00Z");

    /** Every seeded interview gets a distinct, increasing start time, so ordering is deterministic. */
    private int seeded;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private InterviewSessionRepository sessions;
    @Autowired
    private InterviewTurnRepository turns;
    @Autowired
    private InterviewSummaryRepository summaries;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private UserRepository users;

    private record SignedIn(MockHttpSession session, UUID userId) {
    }

    private SignedIn newUser() throws Exception {
        MockHttpSession session = AuthTestSupport.signupVerified(mvc, mapper, users, "list-" + UUID.randomUUID() + "@example.com", "Secret123");
        String me = mvc.perform(get("/api/auth/me").session(session)).andReturn().getResponse().getContentAsString();
        return new SignedIn(session, UUID.fromString(mapper.readTree(me).get("id").asText()));
    }

    /** Seeds interviews directly (no AI involved), each starting a minute after the previous one. */
    private List<InterviewSession> seed(UUID userId, int count, int answeredEach, boolean complete, boolean summarise) {
        List<InterviewSession> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Instant start = BASE.plusSeconds(60L * seeded++);
            InterviewSession session = sessions.save(InterviewSessionFactory.start(userId, FeedbackMode.INSTANT, 5,
                    "Role " + i, ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q1", start));
            var seededTurns = new ArrayList<za.ac.skiet.vyou2.domain.InterviewTurn>();
            for (int n = 1; n <= answeredEach; n++) {
                seededTurns.add(turns.save(InterviewTurnFactory.create(session.getId(), n, "Q" + n, "Answer " + n,
                        EvaluationFactory.create(6, 7, 8, 6 + (n % 2), List.of("Good"), List.of("Better")), start)));
            }
            if (complete) {
                session = sessions.save(InterviewSessionFactory.complete(sessions.findById(session.getId()).orElseThrow(), start.plusSeconds(30)));
            }
            if (summarise && !seededTurns.isEmpty()) {
                summaries.save(InterviewSummaryFactory.create(session.getId(), seededTurns, "Feedback", List.of("A"), List.of("B"), start));
            }
            created.add(session);
        }
        return created;
    }

    private JsonNode page(SignedIn user, String query) throws Exception {
        String body = mvc.perform(get("/api/interviews/mine" + query).session(user.session()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void pagesTheCallersInterviewsNewestFirst() throws Exception {
        SignedIn user = newUser();
        seed(user.userId(), 12, 0, false, false);

        JsonNode first = page(user, "");
        assertThat(first.get("items")).hasSize(10);
        assertThat(first.get("totalItems").asLong()).isEqualTo(12);
        assertThat(first.get("totalPages").asInt()).isEqualTo(2);
        assertThat(first.get("items").get(0).get("jobRole").asText()).isEqualTo("Role 11");
        assertThat(first.get("items").get(9).get("jobRole").asText()).isEqualTo("Role 2");

        JsonNode second = page(user, "?page=1");
        assertThat(second.get("items")).hasSize(2);
        assertThat(second.get("items").get(1).get("jobRole").asText()).isEqualTo("Role 0");
    }

    @Test
    void reportsAnsweredCountsAndAverageScores() throws Exception {
        SignedIn user = newUser();
        seed(user.userId(), 1, 2, true, true);
        seed(user.userId(), 1, 3, false, false);

        JsonNode items = page(user, "").get("items");
        JsonNode inProgress = items.get(0);
        JsonNode completed = items.get(1);
        assertThat(inProgress.get("questionsAnswered").asInt()).isEqualTo(3);
        assertThat(inProgress.get("averageOverall").isNull()).isTrue();
        assertThat(completed.get("questionsAnswered").asInt()).isEqualTo(2);
        assertThat(completed.get("averageOverall").asDouble()).isEqualTo(6.5);
        // The list never carries question, answer or feedback text.
        assertThat(completed.has("currentQuestion")).isFalse();
        assertThat(completed.toString()).doesNotContain("Answer 1").doesNotContain("Feedback");
    }

    @Test
    void filtersByStatus() throws Exception {
        SignedIn user = newUser();
        seed(user.userId(), 3, 1, true, true);
        seed(user.userId(), 2, 1, false, false);

        assertThat(page(user, "?status=COMPLETED").get("totalItems").asLong()).isEqualTo(3);
        assertThat(page(user, "?status=IN_PROGRESS").get("totalItems").asLong()).isEqualTo(2);
        assertThat(page(user, "").get("totalItems").asLong()).isEqualTo(5);
    }

    @Test
    void neverIncludesAnotherUsersInterviews() throws Exception {
        SignedIn owner = newUser();
        SignedIn other = newUser();
        seed(owner.userId(), 4, 1, false, false);

        assertThat(page(other, "").get("totalItems").asLong()).isZero();
        assertThat(page(other, "").get("items")).isEmpty();
    }

    @Test
    void rejectsOutOfRangeOrInvalidParameters() throws Exception {
        SignedIn user = newUser();
        mvc.perform(get("/api/interviews/mine?size=51").session(user.session())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/interviews/mine?size=0").session(user.session())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/interviews/mine?page=-1").session(user.session())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/interviews/mine?status=DELETED").session(user.session())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/interviews/mine?size=50").session(user.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void queryCountDoesNotGrowWithTheNumberOfInterviews() throws Exception {
        // Both users fill the first page (so Spring Data runs its total-count query for both; it
        // skips it for a partial page). Per-row lookups would make the 30-interview user cost more.
        SignedIn eleven = newUser();
        SignedIn thirty = newUser();
        SignedIn two = newUser();
        seed(eleven.userId(), 11, 2, true, true);
        seed(thirty.userId(), 30, 3, true, true);
        seed(two.userId(), 2, 2, true, true);

        long elevenQueries = statementsFor(eleven);
        assertThat(statementsFor(thirty)).isEqualTo(elevenQueries);
        // account re-check (ActiveAccountFilter) + page + total count + answer counts + averages
        assertThat(elevenQueries).isLessThanOrEqualTo(5);
        assertThat(statementsFor(two)).isLessThanOrEqualTo(5);
    }

    private long statementsFor(SignedIn user) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        page(user, "");
        return stats.getPrepareStatementCount();
    }
}
