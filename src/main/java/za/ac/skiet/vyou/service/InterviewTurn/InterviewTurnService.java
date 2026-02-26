package za.ac.skiet.vyou.service.InterviewTurn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.skiet.vyou.domain.AiEvaluation;
import za.ac.skiet.vyou.domain.Content;
import za.ac.skiet.vyou.domain.InterviewTurn;
import za.ac.skiet.vyou.repository.InterviewTurnRepository;
import za.ac.skiet.vyou.service.ai.AiInterviewService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InterviewTurnService implements IInterviewTurnService {

    private final InterviewTurnRepository repo;
    private final AiInterviewService aiService;

    @Autowired
    public InterviewTurnService(InterviewTurnRepository repo,  AiInterviewService aiService) {
        this.repo = repo;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public InterviewTurn create(InterviewTurn turn) {
        return repo.save(turn);
    }

    @Override
    public InterviewTurn read(UUID id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public InterviewTurn update(InterviewTurn turn) {
        return repo.save(turn);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<InterviewTurn> getAll() {
        return repo.findAll();
    }

    @Override
    public List<InterviewTurn> findByInterviewSessionId(UUID sessionId) {
        return repo.findByInterviewSessionIdOrderBySequenceNumber(sessionId);
    }

    @Transactional
    public InterviewTurn submitAnswer(UUID turnId, String userAnswer) {
        InterviewTurn turn = repo.findById(turnId)
                .orElseThrow(() -> new RuntimeException("Turn not found"));

        // Save candidate answer
        Content answerContent = turn.getAnswerContent();
        if (answerContent == null) answerContent = new Content();
        answerContent.setTextData(userAnswer);
        turn.setAnswerContent(answerContent);

        // Call AI service
        Map<String, Object> aiResponse = aiService
                .getNextQuestionAndEvaluation(
                        turn.getInterviewSession().getJobRole(),
                        turn.getInterviewSession().getLevel(),
                        turn.getQuestionContent().getTextData(),
                        userAnswer
                ).block(); // Blocking for simplicity

        // Map AI evaluation to entity
        Map<String, Object> eval = (Map<String, Object>) aiResponse.get("evaluation");
        if (eval != null) {
            AiEvaluation aiEval = new AiEvaluation();
            aiEval.setOverallScore((Integer) eval.get("overallScore"));
            aiEval.setClarityScore((Integer) eval.get("clarityScore"));
            aiEval.setTechnicalScore((Integer) eval.get("technicalScore"));
            aiEval.setConfidenceScore((Integer) eval.get("confidenceScore"));
            aiEval.setStrengths((String) eval.get("strengths"));
            aiEval.setImprovements((String) eval.get("improvements"));
            turn.setAiEvaluation(aiEval);
        }

        // Optionally, you could update the question for the next turn
        // Map next question from AI if provided
        String nextQuestion = (String) aiResponse.get("nextQuestion");
        if (nextQuestion != null) {
            Content questionContent = turn.getQuestionContent();
            if (questionContent == null) questionContent = new Content();
            questionContent.setTextData(nextQuestion);
            turn.setQuestionContent(questionContent);
        }

        return repo.save(turn);
    }
}

