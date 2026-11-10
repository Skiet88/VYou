package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.exception.AnswerRejectedException;
import za.ac.skiet.vyou2.exception.DomainValidationException;
import za.ac.skiet.vyou2.factory.InterviewSessionFactory;
import za.ac.skiet.vyou2.factory.InterviewTurnFactory;
import za.ac.skiet.vyou2.llm.AnswerEvaluator;
import za.ac.skiet.vyou2.llm.QuestionGenerator;
import za.ac.skiet.vyou2.screening.AnswerScreener;
import za.ac.skiet.vyou2.screening.ScreeningResult;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LLM calls happen outside any database transaction, so a slow model never holds a connection.
 */
@Service
public class DefaultInterviewFacade implements InterviewFacade {

    private final InterviewSessionService sessionService;
    private final InterviewTurnService turnService;
    private final AnswerScreener screener;
    private final AnswerEvaluator evaluator;
    private final QuestionGenerator questionGenerator;
    private final Clock clock;

    public DefaultInterviewFacade(InterviewSessionService sessionService, InterviewTurnService turnService,
                                  AnswerScreener screener, AnswerEvaluator evaluator,
                                  QuestionGenerator questionGenerator, Clock clock) {
        this.sessionService = sessionService;
        this.turnService = turnService;
        this.screener = screener;
        this.evaluator = evaluator;
        this.questionGenerator = questionGenerator;
        this.clock = clock;
    }

    @Override
    public InterviewSession start(String jobRole, ExperienceLevel level, InterviewMode mode) {
        // Validate before spending an LLM request on a bad input.
        InterviewSessionFactory.start(jobRole, level, mode, "placeholder", clock.instant());
        String firstQuestion = questionGenerator.firstQuestion(jobRole.strip(), level.name());
        return sessionService.save(InterviewSessionFactory.start(jobRole, level, mode, firstQuestion, clock.instant()));
    }

    @Override
    public AnswerOutcome submitAnswer(UUID sessionId, String answer) {
        InterviewSession session = sessionService.get(sessionId);
        if (!session.isInProgress()) {
            throw new DomainValidationException("Interview session " + sessionId + " is already completed");
        }
        String question = session.getPendingQuestion()
                .orElseThrow(() -> new DomainValidationException("Session has no pending question"));

        ScreeningResult screening = screener.screen(question, answer);
        if (!screening.passed()) {
            throw new AnswerRejectedException(screening.reasons());
        }

        List<InterviewTurn> previous = turnService.forSession(sessionId);
        Evaluation evaluation = evaluator.evaluate(session, question, answer);
        InterviewTurn turn = turnService.record(InterviewTurnFactory.create(
                sessionId, previous.size() + 1, question, answer, evaluation, clock.instant()));

        List<InterviewTurn> history = new ArrayList<>(previous);
        history.add(turn);
        String next = questionGenerator.nextQuestion(session, history);
        InterviewSession updated = sessionService.save(InterviewSessionFactory.withNextQuestion(session, next));
        return new AnswerOutcome(turn, updated);
    }

    @Override
    public InterviewSession complete(UUID sessionId) {
        InterviewSession session = sessionService.get(sessionId);
        return sessionService.save(InterviewSessionFactory.complete(session, clock.instant()));
    }

    @Override
    public SessionDetails details(UUID sessionId) {
        return new SessionDetails(sessionService.get(sessionId), turnService.forSession(sessionId));
    }
}
