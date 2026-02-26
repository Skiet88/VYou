package za.ac.skiet.vyou.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import za.ac.skiet.vyou.service.ai.AiInterviewService;

import java.util.Map;
import java.util.Scanner;

@Component
public class AiInterviewRunner implements CommandLineRunner {

    @Autowired
    private AiInterviewService aiService;

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter role (e.g., Junior Software Developer): ");
        String role = scanner.nextLine();

        System.out.println("Enter level (e.g., Graduate): ");
        String level = scanner.nextLine();

        System.out.println("\n=== AI Interview Started ===");

        String previousQuestion = "";
        String candidateAnswer = "";

        int turns = 3;
        for (int i = 1; i <= turns; i++) {
            int retryCount = 0;
            int maxRetries = 3;
            boolean success = false;

            while (!success && retryCount <= maxRetries) {
                try {
                    Mono<Map<String, Object>> responseMono = aiService
                            .getNextQuestionAndEvaluation(role, level, previousQuestion, candidateAnswer);

                    Map<String, Object> aiResponse = responseMono.block();

                    if (aiResponse == null) {
                        System.out.println("AI did not respond. Ending interview.");
                        return;
                    }

                    System.out.println("\n=== Turn " + i + " ===");

                    String nextQuestion = "";
                    if (aiResponse.containsKey("choices")) {
                        Object choicesObj = aiResponse.get("choices");
                        if (choicesObj instanceof Iterable<?> choices) {
                            for (Object choice : choices) {
                                if (choice instanceof Map<?, ?> choiceMap) {
                                    Map<?, ?> message = (Map<?, ?>) choiceMap.get("message");
                                    if (message != null && message.containsKey("content")) {
                                        nextQuestion = message.get("content").toString();
                                        System.out.println("AI: " + nextQuestion);
                                    }
                                }
                            }
                        }
                    }

                    // User input
                    System.out.print("You: ");
                    candidateAnswer = scanner.nextLine();

                    previousQuestion = nextQuestion;

                    Thread.sleep(1500); // short sleep
                    success = true;      // move to next turn

                } catch (WebClientResponseException.TooManyRequests e) {
                    retryCount++;
                    int waitTime = 2000 * retryCount; // exponential backoff
                    System.out.println("429 Too Many Requests. Waiting " + waitTime + "ms before retry...");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("Error calling AI: " + e.getMessage());
                    return;
                }
            }

            if (!success) {
                System.out.println("Failed to get AI response after " + maxRetries + " retries. Skipping turn.");
            }
        }

        System.out.println("\n=== AI Interview Ended ===");
    }
}
