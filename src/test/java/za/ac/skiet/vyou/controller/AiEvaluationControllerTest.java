package za.ac.skiet.vyou.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import za.ac.skiet.vyou.domain.AiEvaluation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AiEvaluationControllerTest {

    private static final String BASE_URL = "/ai-evaluation";

    @Autowired
    private TestRestTemplate restTemplate;

    private static AiEvaluation evaluation;

    @BeforeAll
    static void setUp() {
        evaluation = new AiEvaluation();
        evaluation.setConfidenceScore(7);
        evaluation.setClarityScore(8);
        evaluation.setTechnicalScore(6);
        evaluation.setOverallScore(7);
        evaluation.setStrengths("Good communication");
        evaluation.setImprovements("Improve technical depth");
    }

    @Test
    @Order(1)
    void a_create() {
        ResponseEntity<AiEvaluation> response =
                restTemplate.postForEntity(BASE_URL, evaluation, AiEvaluation.class);

        assertNotNull(response.getBody());
        evaluation = response.getBody();
        assertNotNull(evaluation.getId());

        System.out.println("Created AI Evaluation: " + evaluation);
    }

    @Test
    @Order(2)
    void b_read() {
        ResponseEntity<AiEvaluation> response =
                restTemplate.getForEntity(BASE_URL + "/" + evaluation.getId(), AiEvaluation.class);

        assertNotNull(response.getBody());
        assertEquals(evaluation.getId(), response.getBody().getId());
    }

    @Test
    @Order(3)
    void c_update() {
        evaluation.setOverallScore(9);

        HttpEntity<AiEvaluation> entity = new HttpEntity<>(evaluation);
        ResponseEntity<AiEvaluation> response =
                restTemplate.exchange(
                        BASE_URL + "/" + evaluation.getId(),
                        HttpMethod.PUT,
                        entity,
                        AiEvaluation.class
                );

        assertEquals(9, response.getBody().getOverallScore());
    }

    @Test
    @Order(4)
    void d_getAll() {
        ResponseEntity<List> response =
                restTemplate.getForEntity(BASE_URL, List.class);

        assertNotNull(response.getBody());
    }

    @Test
    @Order(5)
    void e_delete() {
        restTemplate.delete(BASE_URL + "/" + evaluation.getId());
    }
}
