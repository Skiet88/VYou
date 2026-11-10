package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);
}
