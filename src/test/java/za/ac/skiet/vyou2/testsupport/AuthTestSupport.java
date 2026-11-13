package za.ac.skiet.vyou2.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared signup/login helper so each test exercises the real auth endpoints, not a shortcut. */
public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static MockHttpSession signup(MockMvc mvc, ObjectMapper mapper, String email, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new SignupBody(email, "Test User", password))))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /**
     * Signs up and immediately marks the account verified, bypassing the email link, for tests
     * unrelated to email verification that need full access to the app right after signup.
     */
    public static MockHttpSession signupVerified(MockMvc mvc, ObjectMapper mapper, UserRepository users,
                                                  String email, String password) throws Exception {
        MockHttpSession session = signup(mvc, mapper, email, password);
        var account = users.findByEmail(UserFactory.normaliseEmail(email)).orElseThrow();
        users.save(UserFactory.verifyEmail(account));
        return session;
    }

    private record SignupBody(String email, String displayName, String password) {
    }
}
