package za.ac.skiet.vyou.service.InterviewSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.skiet.vyou.domain.InterviewSession;
import za.ac.skiet.vyou.repository.InterviewSessionRepository;

import java.util.List;
import java.util.UUID;

@Service
public class InterviewSessionService implements IInterviewSessionService {

    private final InterviewSessionRepository repo;

    @Autowired
    public InterviewSessionService(InterviewSessionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public InterviewSession create(InterviewSession session) {
        return repo.save(session);
    }

    @Override
    public InterviewSession read(UUID id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public InterviewSession update(InterviewSession session) {
        return repo.save(session);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<InterviewSession> getAll() {
        return repo.findAll();
    }

    @Override
    public List<InterviewSession> findByUserId(UUID userId) {
        return repo.findByUserId(userId);
    }
}

