package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewTurn;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InterviewTurnService {

    InterviewTurn record(InterviewTurn turn);

    List<InterviewTurn> forSession(UUID sessionId);

    int countForSession(UUID sessionId);

    /** Answered-question counts for many sessions in one query; sessions with none are absent. */
    Map<UUID, Integer> countsForSessions(Collection<UUID> sessionIds);
}
