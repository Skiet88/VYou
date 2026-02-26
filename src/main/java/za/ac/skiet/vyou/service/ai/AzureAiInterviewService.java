//package za.ac.skiet.vyou.service.ai;
//
//import java.util.Arrays;
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import com.azure.ai.inference.ChatCompletionsClient;
//import com.azure.ai.inference.models.ChatCompletions;
//import com.azure.ai.inference.models.ChatCompletionsOptions;
//import com.azure.ai.inference.models.ChatRequestMessage;
//import com.azure.ai.inference.models.ChatRequestSystemMessage;
//import com.azure.ai.inference.models.ChatRequestUserMessage;
//
//@Service
//public class AzureAiInterviewService {
//
//    private final ChatCompletionsClient client;
//    private final String model;
//
//    public AzureAiInterviewService(
//            ChatCompletionsClient client,
//            @Value("${ai.github.model}") String model
//    ) {
//        this.client = client;
//        this.model = model;
//    }
//
//    public String getNextQuestion(String role, String level, String previousAnswer) {
//
//        String systemPrompt = String.format(
//                "You are a professional interviewer for a %s role at %s level. "
//                        + "Ask ONE interview question at a time. "
//                        + "If an answer is provided, briefly evaluate it, then ask the next question.",
//                role, level
//        );
//
//        List<ChatRequestMessage> messages = Arrays.asList(
//                new ChatRequestSystemMessage(systemPrompt),
//                new ChatRequestUserMessage(
//                        previousAnswer == null || previousAnswer.isBlank()
//                                ? "Start the interview with your first question."
//                                : "Candidate answer: " + previousAnswer
//                )
//        );
//
//        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
//        options.setModel(model);
//
//        ChatCompletions completions = client.complete(options);
//
//        return completions
//                .getChoices()
//                .get(0)
//                .getMessage()
//                .getContent();
//    }
//}
