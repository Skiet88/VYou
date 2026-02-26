//package za.ac.skiet.vyou.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.azure.ai.inference.ChatCompletionsClient;
//import com.azure.ai.inference.ChatCompletionsClientBuilder;
//import com.azure.core.credential.AzureKeyCredential;
//
//@Configuration
//public class AzureAiConfig {
//
//    @Bean
//    public ChatCompletionsClient chatCompletionsClient(
//            @Value("${ai.github.token}") String token,
//            @Value("${ai.github.endpoint}") String endpoint
//    ) {
//        return new ChatCompletionsClientBuilder()
//                .credential(new AzureKeyCredential(token))
//                .endpoint(endpoint)
//                .buildClient();
//    }
//}
