package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;

import java.util.UUID;

public interface UserService {

    /** @throws za.ac.skiet.vyou2.exception.ConflictException if the email is already registered */
    UserAccount register(String email, String displayName);

    /** @throws za.ac.skiet.vyou2.exception.ResourceNotFoundException if no user has this id */
    UserAccount get(UUID id);
}
