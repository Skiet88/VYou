package za.ac.skiet.vyou.controller;

//import org.junit.jupiter.api.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

//import org.junit.jupiter.api.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import org.springframework.http.*;
import za.ac.skiet.vyou.domain.UserEntity;
import za.ac.skiet.vyou.ENUM.UserRole;
import za.ac.skiet.vyou.service.userService.UserService;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    private final String BASE_URL = "/users";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserService userService;

    static  Random r= new Random();

    private static UserEntity user;

    @BeforeAll
    static void setUp() {
        user = new UserEntity();
        user.setName("Mlungisi Mbuyazi");
        user.setEmail("mlungisi"+ String.valueOf(r.nextInt(1000))+"@example.com");
        user.setPassword("password123");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @Order(1)
    void a_create() {
        String url = BASE_URL;

        ResponseEntity<UserEntity> response = restTemplate.postForEntity(url, user, UserEntity.class);

        assertNotNull(response.getBody());
        user = response.getBody(); // update static reference with saved ID
        assertNotNull(user.getId());
        assertEquals("Mlungisi Mbuyazi", user.getName());

        System.out.println("Created User: " + user);
    }

    @Test
    @Order(2)
    void b_read() {
        assertNotNull(user);

        String url = BASE_URL + "/" + user.getId();
        ResponseEntity<UserEntity> response = restTemplate.getForEntity(url, UserEntity.class);

        assertNotNull(response.getBody());
        assertEquals(user.getId(), response.getBody().getId());

        System.out.println("Read User: " + response.getBody());
    }

    @Test
    @Order(3)
    void c_update() {
        assertNotNull(user);

        UserEntity updatedUser = new UserEntity();
        updatedUser.setId(user.getId());
        updatedUser.setName("Mlungisi Updated");
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPassword(user.getPassword());
        updatedUser.setRole(user.getRole());
        updatedUser.setCreatedAt(user.getCreatedAt());

        String url = BASE_URL + "/" + user.getId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserEntity> entity = new HttpEntity<>(updatedUser, headers);

        ResponseEntity<UserEntity> response = restTemplate.exchange(url, HttpMethod.PUT, entity, UserEntity.class);

        assertNotNull(response.getBody());
        assertEquals("Mlungisi Updated", response.getBody().getName());

        user = response.getBody();

        System.out.println("Updated User: " + user);
    }

    @Test
    @Order(5)
    void d_delete() {
        assertNotNull(user);

        String url = BASE_URL + "/" + user.getId();
        restTemplate.delete(url);

        // Verify deletion
        ResponseEntity<UserEntity> response = restTemplate.getForEntity(BASE_URL + "/" + user.getId(), UserEntity.class);
        assertTrue(response.getStatusCode().is4xxClientError() || response.getBody() == null);

        System.out.println("Deleted User with ID: " + user.getId());
    }

    @Test
    @Order(6)
    void e_getAll() {
        String url = BASE_URL;

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

        assertNotNull(response.getBody());

        System.out.println("All Users: " + response.getBody());
    }

    @Test
    @Order(4)
    void f_getByEmail() {
        // Re-create user first
        user = userService.create(user);

        String url = BASE_URL + "/email/" + user.getEmail();
        ResponseEntity<UserEntity> response = restTemplate.getForEntity(url, UserEntity.class);

        assertNotNull(response.getBody());
        assertEquals(user.getEmail(), response.getBody().getEmail());

        System.out.println("Get User by Email: " + response.getBody());
    }
}