package za.ac.skiet.vyou.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import za.ac.skiet.vyou.ENUM.ContentType;
import za.ac.skiet.vyou.domain.Content;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContentControllerTest {

    private static final String BASE_URL = "/content";

    @Autowired
    private TestRestTemplate restTemplate;

    private static Content content;

    @BeforeAll
    static void setUp() {
        content = new Content();
        content.setContentType(ContentType.TEXT);
        content.setTextData("Explain polymorphism");
    }

    @Test
    @Order(1)
    void a_create() {
        ResponseEntity<Content> response =
                restTemplate.postForEntity(BASE_URL, content, Content.class);

        assertNotNull(response.getBody());
        content = response.getBody();
        assertNotNull(content.getId());
    }

    @Test
    @Order(2)
    void b_read() {
        ResponseEntity<Content> response =
                restTemplate.getForEntity(BASE_URL + "/" + content.getId(), Content.class);

        assertNotNull(response.getBody());
    }

    @Test
    @Order(3)
    void c_update() {
        content.setTextData("Explain encapsulation");

        HttpEntity<Content> entity = new HttpEntity<>(content);
        ResponseEntity<Content> response =
                restTemplate.exchange(
                        BASE_URL + "/" + content.getId(),
                        HttpMethod.PUT,
                        entity,
                        Content.class
                );

        assertEquals("Explain encapsulation", response.getBody().getTextData());
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
        restTemplate.delete(BASE_URL + "/" + content.getId());
    }
}
