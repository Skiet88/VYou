package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.repository.InterviewTurnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
}
