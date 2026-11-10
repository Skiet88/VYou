package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LlmQuestionGenerator implements QuestionGenerator {

    private final LlmClient llmClient;

    public LlmQuestionGenerator(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public String firstQuestion(String jobRole, String level) {
        return ask(List.of(
                ChatMessage.system(PromptTemplates.INTERVIEWER_SYSTEM),
                ChatMessage.user("Role: " + jobRole + "\nLevel: " + level + "\nAsk the opening question.")));
    }

    @Override
    public String nextQuestion(InterviewSession session, List<InterviewTurn> previousTurns) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(PromptTemplates.INTERVIEWER_SYSTEM));
        messages.add(ChatMessage.user("Role: " + session.getJobRole() + "\nLevel: " + session.getLevel()
                + "\nAsk the opening question."));
        for (InterviewTurn turn : previousTurns) {
            messages.add(new ChatMessage("assistant", turn.getQuestion()));
            messages.add(ChatMessage.user(turn.getAnswer()));
        }
        messages.add(ChatMessage.user("Ask the next question."));
        return ask(messages);
    }

    private String ask(List<ChatMessage> messages) {
        return llmClient.complete(messages, false).strip();
    }
}
