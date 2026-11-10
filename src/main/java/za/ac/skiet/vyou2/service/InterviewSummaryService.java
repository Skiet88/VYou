package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSummary;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSummaryService {

    InterviewSummary save(InterviewSummary summary);

    Optional<InterviewSummary> forSession(UUID sessionId);

    /** Average overall score for each session that has a summary, in one query. */
    Map<UUID, Double> averageOverallFor(Collection<UUID> sessionIds);
}
