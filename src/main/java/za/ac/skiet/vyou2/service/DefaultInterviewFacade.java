package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewSummary;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.exception.AnswerRejectedException;
import za.ac.skiet.vyou2.exception.ConflictException;
import za.ac.skiet.vyou2.exception.DomainValidationException;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import za.ac.skiet.vyou2.exception.LlmUnavailableException;
import za.ac.skiet.vyou2.factory.InterviewSessionFactory;
import za.ac.skiet.vyou2.factory.InterviewSummaryFactory;
import za.ac.skiet.vyou2.factory.InterviewTurnFactory;
import za.ac.skiet.vyou2.llm.AnswerEvaluator;
import za.ac.skiet.vyou2.llm.QuestionGenerator;
import za.ac.skiet.vyou2.llm.SummaryDraft;
import za.ac.skiet.vyou2.llm.SummaryGenerator;
import za.ac.skiet.vyou2.screening.AnswerScreener;
import za.ac.skiet.vyou2.screening.ScreeningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * LLM calls happen outside any database transaction, so a slow model never holds a connection.
 * Concurrent writes to the same session are caught by the optimistic lock on the session and the
 * unique (session, sequence) constraint on turns, and surface as 409 Conflict.
 */
@Service
public class DefaultInterviewFacade implements InterviewFacade {

    private static final Logger log = LoggerFactory.getLogger(DefaultInterviewFacade.class);

    private final UserService userService;
    private final InterviewSessionService sessionService;
    private final InterviewTurnService turnService;
    private final InterviewSummaryService summaryService;
    private final AnswerScreener screener;
    private final AnswerEvaluator evaluator;
    private final QuestionGenerator questionGenerator;
    private final SummaryGenerator summaryGenerator;
    private final Clock clock;

    public DefaultInterviewFacade(UserService userService, InterviewSessionService sessionService,
                                  InterviewTurnService turnService, InterviewSummaryService summaryService,
                                  AnswerScreener screener, AnswerEvaluator evaluator,
                                  QuestionGenerator questionGenerator, SummaryGenerator summaryGenerator,
                                  Clock clock) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.turnService = turnService;
        this.summaryService = summaryService;
        this.screener = screener;
        this.evaluator = evaluator;
        this.questionGenerator = questionGenerator;
        this.summaryGenerator = summaryGenerator;
        this.clock = clock;
    }

    @Override
    public InterviewSession start(UUID userId, String jobRole, ExperienceLevel level, InterviewMode mode) {
        userService.get(userId);
        // Validate before spending an LLM request on a bad input.
        InterviewSessionFactory.start(userId, jobRole, level, mode, "placeholder", clock.instant());
        String firstQuestion = questionGenerator.firstQuestion(jobRole.strip(), level.name());
        return sessionService.save(
                InterviewSessionFactory.start(userId, jobRole, level, mode, firstQuestion, clock.instant()));
    }

    @Override
    public AnswerOutcome submitAnswer(UUID sessionId, String answer) {
        InterviewSession session = sessionService.get(sessionId);
        if (!session.isInProgress()) {
            throw new ConflictException("Interview session " + sessionId + " is already completed");
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
        String next;
        try {
            next = questionGenerator.nextQuestion(session, history);
        } catch (LlmUnavailableException | LlmResponseException ex) {
            // The answer and its evaluation are already saved; keep the session usable by
            // falling back to a generic question rather than losing the turn.
            log.warn("Next-question generation failed for session {}: {}", sessionId, ex.getMessage());
            next = "Tell me about a recent challenge you faced in a project and how you handled it.";
        }
        InterviewSession updated = sessionService.save(InterviewSessionFactory.withNextQuestion(session, next));
        return new AnswerOutcome(turn, updated);
    }

    @Override
    public CompletionOutcome complete(UUID sessionId) {
        InterviewSession session = sessionService.get(sessionId);
        if (!session.isInProgress()) {
            throw new ConflictException("Interview session " + sessionId + " is already completed");
        }
        InterviewSession completed = sessionService.save(InterviewSessionFactory.complete(session, clock.instant()));
        Optional<InterviewSummary> summary;
        try {
            summary = Optional.ofNullable(generateSummaryIfPossible(completed));
        } catch (LlmUnavailableException | LlmResponseException ex) {
            log.warn("Summary generation failed for session {}: {}", sessionId, ex.getMessage());
            summary = Optional.empty();
        }
        return new CompletionOutcome(completed, summary);
    }

    @Override
    public InterviewSummary summarise(UUID sessionId) {
        Optional<InterviewSummary> existing = summaryService.forSession(sessionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        InterviewSession session = sessionService.get(sessionId);
        if (session.isInProgress()) {
            throw new ConflictException("Complete the interview before requesting its summary");
        }
        InterviewSummary summary = generateSummaryIfPossible(session);
        if (summary == null) {
            throw new DomainValidationException("No questions were answered, so there is nothing to summarise");
        }
        return summary;
    }

    @Override
    public SessionDetails details(UUID sessionId) {
        return new SessionDetails(sessionService.get(sessionId), turnService.forSession(sessionId),
                summaryService.forSession(sessionId));
    }

    @Override
    public List<InterviewSession> sessionsForUser(UUID userId) {
        userService.get(userId);
        return sessionService.forUser(userId);
    }

    /** @return the saved summary, or null when no questions were answered */
    private InterviewSummary generateSummaryIfPossible(InterviewSession session) {
        List<InterviewTurn> turns = turnService.forSession(session.getId());
        if (turns.isEmpty()) {
            return null;
        }
        SummaryDraft draft = summaryGenerator.summarise(session, turns);
        return summaryService.save(InterviewSummaryFactory.create(session.getId(), turns, draft.feedback(),
                draft.topStrengths(), draft.focusAreas(), clock.instant()));
    }
}
