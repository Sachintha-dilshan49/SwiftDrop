package lk.swiftdrop.auth.reset;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Password reset settings ({@code swiftdrop.password-reset}).
 *
 * @param resetUrl         frontend page that takes {@code ?token=}
 * @param fromEmail        sender address of the reset email
 * @param defaultRecipient when set, stored as {@code users.password_reset_email} for accounts that have none, so every
 *                         reset email goes there (demo setups only; normally empty so the account email is used)
 */
@ConfigurationProperties("swiftdrop.password-reset")
public record PasswordResetProperties(String resetUrl, String fromEmail, String defaultRecipient) {

    public PasswordResetProperties {
        resetUrl = resetUrl == null || resetUrl.isBlank() ? "http://localhost:3000/reset-password" : resetUrl.trim();
        fromEmail = fromEmail == null || fromEmail.isBlank() ? "noreply@swiftdrop.lk" : fromEmail.trim();
        defaultRecipient = defaultRecipient == null || defaultRecipient.isBlank() ? null : defaultRecipient.trim();
    }
}
