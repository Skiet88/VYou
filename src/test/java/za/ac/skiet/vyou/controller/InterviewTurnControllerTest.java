package za.ac.skiet.vyou.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import za.ac.skiet.vyou.DTO.InterviewTurnDTO;
import za.ac.skiet.vyou.domain.*;
import za.ac.skiet.vyou.service.InterviewSession.InterviewSessionService;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InterviewTurnControllerTest {

    private static final String BASE_URL = "/interview-turn";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InterviewSessionService sessionService;

    private static InterviewTurn turn;

    static Random r = new Random();

    @BeforeAll
    static void setUp() {
        // Question
        Content question = new Content();
        question.setTextData("What is dependency injection?");

        // Answer
        Content answer = new Content();
        answer.setTextData("Dependency Injection is a design pattern used to implement IoC.");

        // AI Evaluation
        AiEvaluation evaluation = new AiEvaluation();
        evaluation.setConfidenceScore(8);
        evaluation.setClarityScore(7);
        evaluation.setTechnicalScore(8);
        evaluation.setOverallScore(8);
        evaluation.setStrengths("Clear explanation");
        evaluation.setImprovements("More examples");

        turn = new InterviewTurn();
        turn.setSequenceNumber(1);
        turn.setQuestionContent(question);
        turn.setAnswerContent(answer);
        turn.setAiEvaluation(evaluation);
    }

    @Test
    @Order(1)
    void a_create() {
        InterviewSession session = sessionService.getAll().get(0); // make sure you have at least one session
        turn.setInterviewSession(session);

        ResponseEntity<InterviewTurnDTO> response =
                restTemplate.postForEntity(BASE_URL, turn, InterviewTurnDTO.class);

        assertNotNull(response.getBody());
        turn.setId(response.getBody().getId()); // store the returned ID
        assertNotNull(turn.getId());

        System.out.println("Created Interview Turn DTO: " + response.getBody());
    }

    @Test
    @Order(2)
    void b_read() {
        ResponseEntity<InterviewTurnDTO> response =
                restTemplate.getForEntity(BASE_URL + "/" + turn.getId(), InterviewTurnDTO.class);

        assertNotNull(response.getBody());
        assertEquals(turn.getId(), response.getBody().getId());

        System.out.println("Read Interview Turn DTO: " + response.getBody());
    }

    @Test
    @Order(3)
    void c_update() {
        turn.setSequenceNumber(2);

        HttpEntity<InterviewTurn> entity = new HttpEntity<>(turn);
        ResponseEntity<InterviewTurnDTO> response =
                restTemplate.exchange(
                        BASE_URL + "/" + turn.getId(),
                        HttpMethod.PUT,
                        entity,
                        InterviewTurnDTO.class
                );

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getSequenceNumber());

        System.out.println("Updated Interview Turn DTO: " + response.getBody());
    }

    @Test
    @Order(4)
    void d_getAll() {
        ResponseEntity<List> response =
                restTemplate.getForEntity(BASE_URL, List.class);

        assertNotNull(response.getBody());
        System.out.println("All Interview Turns DTO: " + response.getBody());
    }

    @Test
    @Order(5)
    void e_delete() {
        restTemplate.delete(BASE_URL + "/" + turn.getId());
        System.out.println("Deleted Interview Turn with ID: " + turn.getId());
    }
}
