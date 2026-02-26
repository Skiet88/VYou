package za.ac.skiet.vyou.service.AiEvaluation;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.skiet.vyou.domain.AiEvaluation;
import za.ac.skiet.vyou.repository.AiEvaluationRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AiEvaluationService implements IAiEvaluationService {

    private final AiEvaluationRepository repo;

    @Autowired
    public AiEvaluationService(AiEvaluationRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public AiEvaluation create(AiEvaluation eval) {
        return repo.save(eval);
    }

    @Override
    public AiEvaluation read(UUID id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public AiEvaluation update(AiEvaluation eval) {
        return repo.save(eval);
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
    public List<AiEvaluation> getAll() {
        return repo.findAll();
    }
}

