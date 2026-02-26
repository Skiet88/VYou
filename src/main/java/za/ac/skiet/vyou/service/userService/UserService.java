package za.ac.skiet.vyou.service.userService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.skiet.vyou.domain.UserEntity;
import za.ac.skiet.vyou.repository.UserRepository;
import za.ac.skiet.vyou.service.userService.IUserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService implements IUserService {

    private final UserRepository repo;

    @Autowired
    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public UserEntity create(UserEntity user) {
        user.setCreatedAt(LocalDateTime.now());
        return repo.save(user);
    }

    @Override
    public UserEntity read(UUID id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public UserEntity update(UserEntity user) {
        return repo.save(user);
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
    public List<UserEntity> getAll() {
        return repo.findAll();
    }

    @Override
    public UserEntity findByEmail(String email) {
        return repo.findByEmail(email).orElse(null);
    }
}
