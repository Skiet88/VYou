//package za.ac.skiet.vyou.service.ai;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import za.ac.skiet.vyou.service.ai.AzureAiInterviewService;
//
//@Component
//public class AzureAiInterviewRunner implements CommandLineRunner {
//
//    private final AzureAiInterviewService aiService;
//
//    public AzureAiInterviewRunner(AzureAiInterviewService aiService) {
//        this.aiService = aiService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("=== Azure AI Interview Test ===");
//
//        String role = "Junior Software Developer";
//        String level = "Graduate";
//        String previousAnswer = ""; // empty because this is the first turn
//
//        try {
//            String aiQuestion = aiService.getNextQuestion(role, level, previousAnswer);
//            System.out.println("AI asks: " + aiQuestion);
//        } catch (Exception e) {
//            System.err.println("Error calling AI: " + e.getMessage());
//        }
//
//        System.out.println("=== End of Test ===");
//    }
//}
