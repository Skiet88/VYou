package za.ac.skiet.vyou.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import za.ac.skiet.vyou.domain.InterviewSession;
import za.ac.skiet.vyou.domain.UserEntity;
import za.ac.skiet.vyou.ENUM.InterviewMode;
import za.ac.skiet.vyou.ENUM.InterviewStatus;
import za.ac.skiet.vyou.ENUM.UserRole;
import za.ac.skiet.vyou.service.userService.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InterviewSessionControllerTest {

    private final String BASE_URL = "/interview-session";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserService userService;

    private static InterviewSession session;
    private static UserEntity user;

    static Random r = new Random();

    @BeforeAll
    static void setUp() {
        user = new UserEntity();
        user.setName("Interview User");
        user.setEmail("interview" + r.nextInt(1000) + "@example.com");
        user.setPassword("password123");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());

        session = new InterviewSession();
        session.setJobRole("Software Engineer");
        session.setLevel("Junior");
        session.setMode(InterviewMode.TEXT);
        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
    }

    @Test
    @Order(1)
    void a_create() {
        // FK safety
        user = userService.create(user);
        session.setUser(user);

        ResponseEntity<InterviewSession> response =
                restTemplate.postForEntity(BASE_URL, session, InterviewSession.class);

        assertNotNull(response.getBody());
        session = response.getBody();

        assertNotNull(session.getId());
        assertEquals("Software Engineer", session.getJobRole());
        assertEquals("Junior", session.getLevel());
        assertEquals(InterviewMode.TEXT, session.getMode());

        System.out.println("Created Interview Session: " + session);
    }

    @Test
    @Order(2)
    void b_read() {
        assertNotNull(session);

        String url = BASE_URL + "/" + session.getId();
        ResponseEntity<InterviewSession> response =
                restTemplate.getForEntity(url, InterviewSession.class);

        assertNotNull(response.getBody());
        assertEquals(session.getId(), response.getBody().getId());

        System.out.println("Read Interview Session: " + response.getBody());
    }

    @Test
    @Order(3)
    void c_update() {
        assertNotNull(session);

        InterviewSession updated = new InterviewSession();
        updated.setId(session.getId());
        updated.setUser(session.getUser());
        updated.setJobRole("Cybersecurity Analyst");
        updated.setLevel("Graduate");
        updated.setMode(session.getMode());
        updated.setStatus(InterviewStatus.COMPLETED);
        updated.setStartedAt(session.getStartedAt());
        updated.setEndedAt(LocalDateTime.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InterviewSession> entity = new HttpEntity<>(updated, headers);

        ResponseEntity<InterviewSession> response =
                restTemplate.exchange(
                        BASE_URL + "/" + session.getId(),
                        HttpMethod.PUT,
                        entity,
                        InterviewSession.class
                );

        assertNotNull(response.getBody());
        assertEquals("Cybersecurity Analyst", response.getBody().getJobRole());
        assertEquals(InterviewStatus.COMPLETED, response.getBody().getStatus());

        session = response.getBody();

        System.out.println("Updated Interview Session: " + session);
    }

    @Test
    @Order(4)
    void d_getAll() {
        ResponseEntity<List> response =
                restTemplate.getForEntity(BASE_URL, List.class);

        assertNotNull(response.getBody());

        System.out.println("All Interview Sessions: " + response.getBody());
    }

    @Test
    @Order(5)
    void e_delete() {
        assertNotNull(session);

        restTemplate.delete(BASE_URL + "/" + session.getId());

        ResponseEntity<InterviewSession> response =
                restTemplate.getForEntity(BASE_URL + "/" + session.getId(), InterviewSession.class);

        assertTrue(response.getStatusCode().is4xxClientError() || response.getBody() == null);

        System.out.println("Deleted Interview Session with ID: " + session.getId());
    }
}
