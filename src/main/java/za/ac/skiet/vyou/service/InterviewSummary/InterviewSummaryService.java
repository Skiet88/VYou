package za.ac.skiet.vyou.service.InterviewSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.skiet.vyou.domain.InterviewSummary;
import za.ac.skiet.vyou.repository.InterviewSummaryRepository;

import java.util.List;
import java.util.UUID;

@Service
public class InterviewSummaryService implements IInterviewSummaryService {

    private final InterviewSummaryRepository repo;

    @Autowired
    public InterviewSummaryService(InterviewSummaryRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public InterviewSummary create(InterviewSummary summary) {
        return repo.save(summary);
    }

    @Override
    public InterviewSummary read(UUID id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public InterviewSummary update(InterviewSummary summary) {
        return repo.save(summary);
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
    public List<InterviewSummary> getAll() {
        return repo.findAll();
    }

    @Override
    public InterviewSummary findByInterviewSessionId(UUID sessionId) {
        return repo.findByInterviewSessionId(sessionId).orElse(null);
    }
}

