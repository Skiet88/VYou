package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.exception.InvalidPasswordResetTokenException;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.mail.Mailer;
import za.ac.skiet.vyou2.repository.UserRepository;
import za.ac.skiet.vyou2.util.SecureTokens;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final Mailer mailer;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetServiceImpl(UserRepository users, Mailer mailer, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void requestReset(String email, String frontendOrigin) {
        // Same email either way, or none at all: this must never reveal whether an address is
        // registered (matches login's and signup's generic failure messages).
        String normalised = UserFactory.normaliseEmail(email);
        UserAccount user = users.findByEmail(normalised).orElse(null);
        if (user == null) {
            return;
        }
        String rawToken = SecureTokens.generateRawToken();
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        users.save(UserFactory.withPasswordResetToken(user, SecureTokens.sha256Hex(rawToken), expiresAt));

        String link = frontendOrigin + "/reset-password?token=" + rawToken;
        String body = "Hi " + user.getDisplayName() + ",\n\n"
                + "Reset your VYou password by opening this link:\n" + link + "\n\n"
                + "This link expires in 1 hour. If you didn't request this, you can ignore this email - "
                + "your password hasn't changed.";
        mailer.send(user.getEmail(), "Reset your VYou password", body);
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newRawPassword) {
        UserAccount user = users.findByPasswordResetTokenHash(SecureTokens.sha256Hex(rawToken))
                .orElseThrow(PasswordResetServiceImpl::invalidToken);
        Instant expiresAt = user.getPasswordResetTokenExpiresAt().orElseThrow(PasswordResetServiceImpl::invalidToken);
        if (clock.instant().isAfter(expiresAt)) {
            throw invalidToken();
        }
        users.save(UserFactory.resetPassword(user, passwordEncoder.encode(newRawPassword), clock.instant()));
    }

    private static InvalidPasswordResetTokenException invalidToken() {
        return new InvalidPasswordResetTokenException("That password reset link is invalid or has expired.");
    }
}
