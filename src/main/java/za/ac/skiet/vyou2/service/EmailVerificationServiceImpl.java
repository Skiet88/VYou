package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.exception.InvalidVerificationTokenException;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.mail.Mailer;
import za.ac.skiet.vyou2.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository users;
    private final Mailer mailer;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationServiceImpl(UserRepository users, Mailer mailer, Clock clock) {
        this.users = users;
        this.mailer = mailer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void sendVerificationEmail(UUID userId, String frontendOrigin) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));
        if (user.isEmailVerified()) {
            return;
        }
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        users.save(UserFactory.withVerificationToken(user, sha256Hex(rawToken), expiresAt));

        String link = frontendOrigin + "/verify-email?token=" + rawToken;
        String body = "Hi " + user.getDisplayName() + ",\n\n"
                + "Verify your email address for VYou by opening this link:\n" + link + "\n\n"
                + "This link expires in 24 hours. If you didn't create a VYou account, you can ignore this email.";
        mailer.send(user.getEmail(), "Verify your VYou email address", body);
    }

    @Override
    @Transactional
    public void verify(String rawToken) {
        UserAccount user = users.findByVerificationTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> invalidToken());
        Instant expiresAt = user.getVerificationTokenExpiresAt().orElseThrow(() -> invalidToken());
        if (clock.instant().isAfter(expiresAt)) {
            throw invalidToken();
        }
        users.save(UserFactory.verifyEmail(user));
    }

    private static InvalidVerificationTokenException invalidToken() {
        return new InvalidVerificationTokenException("That verification link is invalid or has expired.");
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
