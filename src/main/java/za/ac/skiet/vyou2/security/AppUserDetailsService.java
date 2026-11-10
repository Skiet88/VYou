package za.ac.skiet.vyou2.security;

import za.ac.skiet.vyou2.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a user for Spring Security by email. Spring Security itself checks
 * {@link AppUserDetails#isEnabled()} (backed by {@code UserStatus}) on every authentication,
 * so a suspended user is rejected automatically.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    public AppUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email.strip().toLowerCase())
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with that email"));
    }
}
