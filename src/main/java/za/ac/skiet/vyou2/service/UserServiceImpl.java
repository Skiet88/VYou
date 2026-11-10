package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.exception.ConflictException;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserAccount register(String email, String displayName, String rawPassword) {
        String normalisedEmail = UserFactory.normaliseEmail(email);
        if (repository.existsByEmail(normalisedEmail)) {
            // Same message/status as any other validation failure: do not reveal that the
            // email specifically is the problem (sign-up enumeration).
            throw new ConflictException("Could not create an account with the details provided");
        }
        UserAccount user = UserFactory.register(normalisedEmail, displayName,
                passwordEncoder.encode(rawPassword), clock.instant());
        return repository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(UserFactory.normaliseEmail(email));
    }
}
