package za.ac.skiet.vyou.service.ai;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AiInterviewService {

    private final WebClient webClient;

    public AiInterviewService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Sends the candidate answer to OpenAI and retrieves the next question and structured evaluation.
     *
     * @param role     Role of the interview (e.g., Software Developer)
     * @param level    Level of the candidate (e.g., Graduate)
     * @param question Previous question (can be optional, AI can track context)
     * @param answer   Candidate's answer to the previous question
     * @return Mono of Map containing AI's structured response
     */
    public Mono<Map<String, Object>> getNextQuestionAndEvaluation(String role, String level, String question, String answer) {

        // System prompt to instruct AI
        String systemPrompt = String.format(
                "You are a professional interviewer for a %s role, level %s. "
                        + "Ask one question at a time, and evaluate the candidate's answer in terms of clarity, technical skill, and confidence. "
                        + "Return the next question and a structured evaluation in JSON format.",
                role, level
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-5.2-chat-latest",   // updated to latest GPT model
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Candidate Answer: " + answer)
                },
                "temperature", 0.7
        );

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnNext(response -> {
                    // Debug logging for AI response
                    System.out.println("===== AI RAW RESPONSE =====");
                    System.out.println(response);
                    System.out.println("===========================");
                });
    }
}
