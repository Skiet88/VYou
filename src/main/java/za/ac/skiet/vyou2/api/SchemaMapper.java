package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.schemas.AnswerResponse;
import za.ac.skiet.vyou2.schemas.EvaluationResponse;
import za.ac.skiet.vyou2.schemas.SessionDetailsResponse;
import za.ac.skiet.vyou2.schemas.SessionResponse;
import za.ac.skiet.vyou2.schemas.TurnResponse;
import za.ac.skiet.vyou2.service.AnswerOutcome;
import za.ac.skiet.vyou2.service.SessionDetails;
import za.ac.skiet.vyou2.util.TextUtils;

final class SchemaMapper {

    private SchemaMapper() {
    }

    static SessionResponse toResponse(InterviewSession s) {
        return new SessionResponse(s.getId(), s.getJobRole(), s.getLevel(), s.getMode(), s.getStatus(),
                s.getPendingQuestion().orElse(null), s.getStartedAt(), s.getEndedAt().orElse(null));
    }

    static TurnResponse toResponse(InterviewTurn t) {
        return new TurnResponse(t.getSequenceNumber(), t.getQuestion(), t.getAnswer(),
                toResponse(t.getEvaluation()), t.getAnsweredAt());
    }

    static EvaluationResponse toResponse(Evaluation e) {
        return new EvaluationResponse(e.getClarityScore(), e.getTechnicalScore(), e.getConfidenceScore(),
                e.getOverallScore(), TextUtils.splitLines(e.getStrengths()), TextUtils.splitLines(e.getImprovements()));
    }

    static AnswerResponse toResponse(AnswerOutcome o) {
        return new AnswerResponse(toResponse(o.turn()), o.session().getPendingQuestion().orElse(null));
    }

    static SessionDetailsResponse toResponse(SessionDetails d) {
        return new SessionDetailsResponse(toResponse(d.session()),
                d.turns().stream().map(SchemaMapper::toResponse).toList());
    }
}
