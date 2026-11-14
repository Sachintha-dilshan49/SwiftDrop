package lk.swiftdrop.auth.reset;

import lk.swiftdrop.auth.account.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * On start, fills {@code users.password_reset_email} for accounts that have none when
 * {@code AUTH_PASSWORD_RESET_DEFAULT_EMAIL} is set. Off by default; accounts that already have an address keep it.
 */
@Component
@EnableConfigurationProperties(PasswordResetProperties.class)
public class PasswordResetEmailDefaults implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailDefaults.class);

    private final UserRepository userRepository;
    private final PasswordResetProperties properties;

    public PasswordResetEmailDefaults(UserRepository userRepository, PasswordResetProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.defaultRecipient() == null) {
            return;
        }
        int updated = userRepository.fillMissingPasswordResetEmail(properties.defaultRecipient());
        if (updated > 0) {
            log.info("Set the password reset address for {} accounts", updated);
        }
    }
}
