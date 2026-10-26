package lk.swiftdrop.notification.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailChannel(JavaMailSender mailSender,
                        @Value("${swiftdrop.notification.from-email}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        try {
            var message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent for subject: {}", subject);
        } catch (MailException e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new NotificationSendException("Email send failed", e);
        }
    }

    @Override
    public String channelName() { return "EMAIL"; }
}
