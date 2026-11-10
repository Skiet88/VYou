package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.exception.ConflictException;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final Clock clock;

    public UserServiceImpl(UserRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserAccount register(String email, String displayName) {
        UserAccount user = UserFactory.create(email, displayName, clock.instant());
        if (repository.existsByEmail(user.getEmail())) {
            throw new ConflictException("A user with this email already exists");
        }
        return repository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }
}
