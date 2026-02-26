package za.ac.skiet.vyou.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import za.ac.skiet.vyou.DTO.InterviewSummaryDTO;
import za.ac.skiet.vyou.domain.*;
import za.ac.skiet.vyou.ENUM.UserRole;
import za.ac.skiet.vyou.ENUM.InterviewMode;
import za.ac.skiet.vyou.ENUM.InterviewStatus;
import za.ac.skiet.vyou.service.userService.UserService;
import za.ac.skiet.vyou.service.InterviewSession.InterviewSessionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InterviewSummaryControllerTest {

    private static final String BASE_URL = "/interview-summary";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InterviewSessionService sessionService;

    @Autowired
    private UserService userService;

    private static InterviewSummary summary;
    private static InterviewSession session;
    private static UserEntity user;

    static Random r = new Random();

    @BeforeAll
    static void setUp(@Autowired UserService userService,
                      @Autowired InterviewSessionService sessionService) {

        // 1️⃣ Create a User
        user = new UserEntity();
        user.setName("Test User");
        user.setEmail("testuser" + r.nextInt(1000) + "@example.com");
        user.setPassword("password123");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());

        user = userService.create(user); // Save to DB

        // 2️⃣ Create an InterviewSession
        session = new InterviewSession();
        session.setUser(user);
        session.setJobRole("Software Engineer");
        session.setLevel("Junior");
        session.setMode(InterviewMode.TEXT);
        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());

        session = sessionService.create(session); // Save to DB

        // 3️⃣ Prepare InterviewSummary
        summary = new InterviewSummary();
        summary.setInterviewSession(session);
        summary.setOverallScore(75);
        summary.setGeneralFeedback("Strong fundamentals");
        summary.setRecommendations("Practice system design");
        summary.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @Order(1)
    void a_create() {
        ResponseEntity<InterviewSummary> response =
                restTemplate.postForEntity(BASE_URL, summary, InterviewSummary.class);

        assertNotNull(response.getBody());
        summary = response.getBody();
        assertNotNull(summary.getId());

        System.out.println("Created InterviewSummary: " + summary);
    }

    @Test
    @Order(2)
    void b_read() {
        ResponseEntity<InterviewSummary> response =
                restTemplate.getForEntity(BASE_URL + "/" + summary.getId(), InterviewSummary.class);

        assertNotNull(response.getBody());
        assertEquals(summary.getId(), response.getBody().getId());

        System.out.println("Read InterviewSummary: " + response.getBody());
    }

    @Test
    @Order(3)
    void c_update() {
        summary.setOverallScore(85);

        HttpEntity<InterviewSummary> entity = new HttpEntity<>(summary);
        ResponseEntity<InterviewSummaryDTO> response =
                restTemplate.exchange(
                        BASE_URL + "/" + summary.getId(),
                        HttpMethod.PUT,
                        entity,
                        InterviewSummaryDTO.class
                );

        assertNotNull(response.getBody());
        assertEquals(85, response.getBody().getOverallScore());

        System.out.println("Updated Interview Summary DTO: " + response.getBody());
    }


    @Test
    @Order(4)
    void d_getAll() {
        ResponseEntity<List> response =
                restTemplate.getForEntity(BASE_URL, List.class);

        assertNotNull(response.getBody());
        System.out.println("All Interview Summaries DTOs: " + response.getBody());
    }


    @Test
    @Order(5)
    void e_delete() {
        restTemplate.delete(BASE_URL + "/" + summary.getId());

        ResponseEntity<InterviewSummary> response =
                restTemplate.getForEntity(BASE_URL + "/" + summary.getId(), InterviewSummary.class);

        assertTrue(response.getStatusCode().is4xxClientError() || response.getBody() == null);
        System.out.println("Deleted InterviewSummary with ID: " + summary.getId());
    }
}
