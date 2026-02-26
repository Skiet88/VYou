package za.ac.skiet.vyou.service.Content;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.skiet.vyou.domain.Content;
import za.ac.skiet.vyou.repository.ContentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ContentService implements IContentService {

    private final ContentRepository repo;

    @Autowired
    public ContentService(ContentRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Content create(Content content) {
        return repo.save(content);
    }

    @Override
    public Content read(UUID id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Content update(Content content) {
        return repo.save(content);
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
    public List<Content> getAll() {
        return repo.findAll();
    }
}

