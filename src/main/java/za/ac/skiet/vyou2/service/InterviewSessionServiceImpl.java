package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionRepository repository;

    public InterviewSessionServiceImpl(InterviewSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InterviewSession save(InterviewSession session) {
        return repository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSession get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewSession> forUser(UUID userId) {
        return repository.findByUserIdOrderByStartedAtDesc(userId);
    }
}
