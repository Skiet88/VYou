package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.repository.InterviewTurnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InterviewTurnServiceImpl implements InterviewTurnService {

    private final InterviewTurnRepository repository;

    public InterviewTurnServiceImpl(InterviewTurnRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InterviewTurn record(InterviewTurn turn) {
        return repository.save(turn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewTurn> forSession(UUID sessionId) {
        return repository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countForSession(UUID sessionId) {
        return repository.countBySessionId(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Integer> countsForSessions(Collection<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        return repository.countBySessionIds(sessionIds).stream().collect(Collectors.toMap(
                InterviewTurnRepository.SessionTurnCount::getSessionId, c -> (int) c.getAnswered()));
    }
}
