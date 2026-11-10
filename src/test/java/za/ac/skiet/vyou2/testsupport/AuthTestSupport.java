package za.ac.skiet.vyou2.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

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

    private record SignupBody(String email, String displayName, String password) {
    }
}
