package lk.swiftdrop.auth.reset;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forgot password: email a single-use link, then set a new password with it.
 *
 * <p>The request answers the same way whether or not the account exists, so it cannot be used to find out who has an
 * account. The link goes to {@code users.password_reset_email} when it is set, otherwise to the account email.
 */
@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    /** At most one email per account per minute. */
    static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    static final String INVALID_LINK = "This reset link is invalid or has expired. Ask for a new one.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final ApplicationEventPublisher events;
    private final PasswordResetProperties properties;
    private final Clock clock;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository resetTokenRepository,
                                RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                                JwtTokenService tokenService, ApplicationEventPublisher events,
                                PasswordResetProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmailIgnoreCase(normalized)
                .filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
                .orElse(null);
        if (user == null) {
            return;
        }
        Instant now = clock.instant();
        if (resetTokenRepository.existsUnusedSince(user.getId(), now.minus(RESEND_INTERVAL))) {
            return;
        }
        if (user.getPasswordResetEmail() == null && properties.defaultRecipient() != null) {
            user.setPasswordResetEmail(properties.defaultRecipient());
        }
        resetTokenRepository.invalidateAllByUserId(user.getId(), now);
        String rawToken = tokenService.createRefreshToken();
        resetTokenRepository.save(new PasswordResetTokenEntity(
                user, tokenService.hashRefreshToken(rawToken), now, now.plus(TOKEN_TTL)));
        String firstName = user.getFullName().trim().split("\\s+", 2)[0];
        events.publishEvent(new PasswordResetRequested(user.passwordResetRecipient(), firstName, resetLink(rawToken)));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetTokenEntity token = resetTokenRepository
                .findByTokenHashForUpdate(tokenService.hashRefreshToken(rawToken.trim()))
                .orElseThrow(() -> new ValidationException(INVALID_LINK));
        Instant now = clock.instant();
        UserEntity user = token.getUser();
        if (!token.isUsableAt(now) || user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException(INVALID_LINK);
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        resetTokenRepository.invalidateAllByUserId(user.getId(), now);
        token.markUsed(now);
        // Sign out every device that used the old password
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
    }

    private String resetLink(String rawToken) {
        String separator = properties.resetUrl().contains("?") ? "&" : "?";
        return properties.resetUrl() + separator + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
