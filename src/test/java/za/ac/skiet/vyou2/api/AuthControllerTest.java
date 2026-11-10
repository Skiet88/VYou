package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.UserStatus;
import za.ac.skiet.vyou2.testsupport.AuthTestSupport;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Clock clock;

    @Test
    void signupThenMeReturnsTheNewAccount() throws Exception {
        String email = "signup-" + UUID.randomUUID() + "@example.com";
        var session = AuthTestSupport.signup(mvc, mapper, email, "Secret123");

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void meWithoutASessionIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithAGenericMessage() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody(email, "WrongPassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginWithAnUnknownEmailGivesTheSameGenericMessage() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody("nobody-" + UUID.randomUUID() + "@example.com", "Whatever1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void duplicateSignupIsRejectedWithoutConfirmingTheEmailExists() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        AuthTestSupport.signup(mvc, mapper, email, "Secret123");

        mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                java.util.Map.of("email", email, "displayName", "Someone Else", "password", "Secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Could not create an account with the details provided"));
    }

    @Test
    void stateChangingRequestWithoutACsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody("a@example.com", "Secret123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedUserCannotLogIn() throws Exception {
        String email = "suspended-" + UUID.randomUUID() + "@example.com";
        var user = UserFactory.register(email, "Suspended", passwordEncoder.encode("Secret123"), clock.instant());
        userRepository.save(UserFactory.withStatus(user, UserStatus.SUSPENDED));

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginBody(email, "Secret123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        var session = AuthTestSupport.signup(mvc, mapper, "logout-" + UUID.randomUUID() + "@example.com", "Secret123");
        mvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    private record LoginBody(String email, String password) {
    }
}
