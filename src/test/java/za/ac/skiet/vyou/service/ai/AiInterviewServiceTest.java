package za.ac.skiet.vyou.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.Map;

@SpringBootTest
class AiInterviewServiceTest {

    @Autowired
    private AiInterviewService aiService;

    @Test
    void testAiInterviewFlow() {
        String role = "Software Developer";
        String level = "Graduate";

        String previousQuestion = "";  // No previous question, AI generates first
        String candidateAnswer = "";   // Empty answer for first turn

        int turns = 3; // simulate 3 AI questions
        for (int i = 1; i <= turns; i++) {
            Mono<Map<String, Object>> responseMono = aiService
                    .getNextQuestionAndEvaluation(role, level, previousQuestion, candidateAnswer);

            Map<String, Object> aiResponse = responseMono.block(); // block() is fine for tests

            assert aiResponse != null;
            System.out.println("=== Turn " + i + " ===");
            System.out.println("Raw AI Response: " + aiResponse);

            // Extract the AI-generated next question (depends on OpenAI response structure)
            String nextQuestion = "";
            if (aiResponse.containsKey("choices")) {
                Object choicesObj = aiResponse.get("choices");
                if (choicesObj instanceof Iterable<?> choices) {
                    for (Object choice : choices) {
                        if (choice instanceof Map<?, ?> choiceMap) {
                            Map<?, ?> message = (Map<?, ?>) choiceMap.get("message");
                            if (message != null && message.containsKey("content")) {
                                nextQuestion = message.get("content").toString();
                                System.out.println("AI Question: " + nextQuestion);
                            }
                        }
                    }
                }
            }

            // Simulate candidate answer (for testing, you can make it dynamic)
            candidateAnswer = "Sample answer for turn " + i;

            // Prepare for next turn
            previousQuestion = nextQuestion;
        }
    }
}
