package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewSummary;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.schemas.AnswerResponse;
import za.ac.skiet.vyou2.schemas.CompletionResponse;
import za.ac.skiet.vyou2.schemas.EvaluationResponse;
import za.ac.skiet.vyou2.schemas.SessionDetailsResponse;
import za.ac.skiet.vyou2.schemas.SessionResponse;
import za.ac.skiet.vyou2.schemas.SummaryResponse;
import za.ac.skiet.vyou2.schemas.TurnResponse;
import za.ac.skiet.vyou2.schemas.UserResponse;
import za.ac.skiet.vyou2.service.AnswerOutcome;
import za.ac.skiet.vyou2.service.InterviewFacade.CompletionOutcome;
import za.ac.skiet.vyou2.service.InterviewFacade.SessionOverview;
import za.ac.skiet.vyou2.service.SessionDetails;
import za.ac.skiet.vyou2.util.TextUtils;

final class SchemaMapper {

    private SchemaMapper() {
    }

    static SessionResponse toResponse(InterviewSession s, int questionsAnswered) {
        return new SessionResponse(s.getId(), s.getJobRole(), s.getLevel(), s.getMode(), s.getStatus(),
                s.getFeedbackMode(), s.getTotalQuestions(), questionsAnswered,
                s.getPendingQuestion().orElse(null), s.getStartedAt(), s.getEndedAt().orElse(null));
    }

    static SessionResponse toResponse(SessionOverview o) {
        return toResponse(o.session(), o.questionsAnswered());
    }

    /** {@code revealEvaluation} is false while the session is in END_OF_INTERVIEW mode and still in progress. */
    private static TurnResponse toResponse(InterviewTurn t, boolean revealEvaluation) {
        return new TurnResponse(t.getSequenceNumber(), t.getQuestion(), t.getAnswer(),
                revealEvaluation ? toResponse(t.getEvaluation()) : null, t.getAnsweredAt());
    }

    private static EvaluationResponse toResponse(Evaluation e) {
        return new EvaluationResponse(e.getClarityScore(), e.getTechnicalScore(), e.getConfidenceScore(),
                e.getOverallScore(), TextUtils.splitLines(e.getStrengths()), TextUtils.splitLines(e.getImprovements()));
    }

    static AnswerResponse toResponse(AnswerOutcome o) {
        boolean reveal = reveals(o.session());
        return new AnswerResponse(toResponse(o.turn(), reveal), o.session().getPendingQuestion().orElse(null),
                o.session().getStatus());
    }

    static SessionDetailsResponse toResponse(SessionDetails d) {
        boolean reveal = reveals(d.session());
        return new SessionDetailsResponse(toResponse(d.session(), d.turns().size()),
                d.turns().stream().map(t -> toResponse(t, reveal)).toList(),
                d.summary().map(SchemaMapper::toResponse).orElse(null));
    }

    static CompletionResponse toResponse(CompletionOutcome o) {
        return new CompletionResponse(toResponse(o.session(), o.questionsAnswered()),
                o.summary().map(SchemaMapper::toResponse).orElse(null));
    }

    static SummaryResponse toResponse(InterviewSummary s) {
        return new SummaryResponse(s.getQuestionsAnswered(), s.getAverageClarity(), s.getAverageTechnical(),
                s.getAverageConfidence(), s.getAverageOverall(), s.getFeedback(),
                TextUtils.splitLines(s.getTopStrengths()), TextUtils.splitLines(s.getFocusAreas()), s.getCreatedAt());
    }

    static UserResponse toResponse(UserAccount u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getCreatedAt());
    }

    /** Scores are revealed per-turn once feedback is INSTANT, or once the interview is no longer in progress. */
    private static boolean reveals(InterviewSession session) {
        return session.getFeedbackMode() == za.ac.skiet.vyou2.domain.FeedbackMode.INSTANT || !session.isInProgress();
    }
}
