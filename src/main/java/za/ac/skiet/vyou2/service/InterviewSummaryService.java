package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSummary;

import java.util.Optional;
import java.util.UUID;

public interface InterviewSummaryService {

    InterviewSummary save(InterviewSummary summary);

    Optional<InterviewSummary> forSession(UUID sessionId);
}
