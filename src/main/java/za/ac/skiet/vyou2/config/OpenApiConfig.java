package za.ac.skiet.vyou2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API docs at /swagger-ui.html, spec at /v3/api-docs (the contract for the frontend). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vyouOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VYou2 API")
                .version("v1")
                .description("AI interview simulator: users, interview sessions, GPT-4o answer evaluation and summaries."));
    }
}
