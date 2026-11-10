package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSummary;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
}
