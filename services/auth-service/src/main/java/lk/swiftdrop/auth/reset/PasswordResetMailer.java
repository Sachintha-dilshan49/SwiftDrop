package lk.swiftdrop.auth.reset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends the reset link. A mail failure is logged (without the address or link) and never fails the request. */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;

    public PasswordResetMailer(JavaMailSender mailSender, PasswordResetProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(PasswordResetRequested event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromEmail());
        message.setTo(event.recipient());
        message.setSubject("Reset your SwiftDrop password");
        message.setText("""
                Hi %s,

                We received a request to reset your SwiftDrop password. Open this link to choose a new one:

                %s

                The link works once and expires in %d minutes. If you did not ask for this, ignore this email; \
                your password stays the same.

                SwiftDrop
                """.formatted(event.firstName(), event.resetLink(), PasswordResetService.TOKEN_TTL.toMinutes()));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Password reset email could not be sent: {}", exception.getClass().getSimpleName());
        }
    }
}
