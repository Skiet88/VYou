//package za.ac.skiet.vyou.service.ai;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.beans.factory.annotation.Autowired;
//import reactor.core.publisher.Mono;
//
//import java.util.Map;
//
//@Component
//public class SingleAiRunner implements CommandLineRunner {
//
//    private final AiInterviewService aiService;
//
//    @Autowired
//    public SingleAiRunner(AiInterviewService aiService) {
//        this.aiService = aiService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        String role = "Software Developer";
//        String level = "Graduate";
//        String previousQuestion = "";  // first request, no previous question
//        String candidateAnswer = "";   // first turn, no answer yet
//
//        System.out.println("=== Sending single AI request ===");
//
//        try {
//            Mono<Map<String, Object>> responseMono = aiService
//                    .getNextQuestionAndEvaluation(role, level, previousQuestion, candidateAnswer);
//
//            // block() is fine here since this is a simple runner
//            Map<String, Object> aiResponse = responseMono.block();
//
//            if (aiResponse != null) {
//                System.out.println("AI Response:");
//                System.out.println(aiResponse);
//            } else {
//                System.out.println("No response from AI.");
//            }
//        } catch (Exception e) {
//            System.err.println("Error calling AI: " + e.getMessage());
//        }
//    }
//}
