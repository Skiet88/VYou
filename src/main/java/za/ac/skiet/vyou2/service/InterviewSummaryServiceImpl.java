package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSummary;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InterviewSummaryServiceImpl implements InterviewSummaryService {

    private final InterviewSummaryRepository repository;

    public InterviewSummaryServiceImpl(InterviewSummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InterviewSummary save(InterviewSummary summary) {
        return repository.save(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InterviewSummary> forSession(UUID sessionId) {
        return repository.findBySessionId(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Double> averageOverallFor(Collection<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        return repository.averagesForSessions(sessionIds).stream().collect(Collectors.toMap(
                InterviewSummaryRepository.SessionAverage::getSessionId,
                InterviewSummaryRepository.SessionAverage::getAverageOverall));
    }
}
