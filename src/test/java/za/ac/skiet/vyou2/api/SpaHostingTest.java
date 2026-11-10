package za.ac.skiet.vyou2.api;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The backend serving the built frontend (here a stand-in index.html and asset on the test
 * classpath). MockMvc doesn't run the servlet container's static file serving, so these check the
 * routing and security decisions; the real files were also checked against a running jar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaHostingTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void clientSideRoutesServeTheAppShellToSignedOutVisitors() throws Exception {
        // "/" is forwarded to index.html by Spring Boot's welcome-page support (MockMvc doesn't follow forwards).
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        for (String path : new String[] {"/login", "/interviews/123e4567-e89b-12d3-a456-426614174000", "/users"}) {
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("SPA_SHELL")))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self'")))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }

    @Test
    void hashedAssetsAreServedAndCachedLongTerm() throws Exception {
        mvc.perform(get("/assets/app-abc123.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
        mvc.perform(get("/login")).andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void missingFilesAreRealNotFoundsNotTheAppShell() throws Exception {
        mvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
        mvc.perform(get("/robots.txt")).andExpect(status().isNotFound());
    }

    @Test
    void apiPathsNeverFallBackToTheAppShellAndStillRequireASession() throws Exception {
        mvc.perform(get("/api/interviews/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("SPA_SHELL"))));
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void encodedPathsCannotSneakAnApiCallThroughAsAPageRequest() throws Exception {
        // "%61" is "a": this is /api/interviews/mine in disguise. It must be treated as an API call.
        // A raw URI, so the "%61" reaches the server exactly as sent (a template would re-encode the %).
        mvc.perform(get(URI.create("/%61pi/interviews/mine"))).andExpect(status().isUnauthorized());
        mvc.perform(get(URI.create("/%61pi/admin/users"))).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyGetIsOpenedUpForPages() throws Exception {
        mvc.perform(post("/interviews").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }
}
