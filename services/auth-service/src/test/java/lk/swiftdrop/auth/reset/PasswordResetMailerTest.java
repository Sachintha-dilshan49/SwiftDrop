package lk.swiftdrop.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class PasswordResetMailerTest {

    @Mock private JavaMailSender mailSender;

    private final PasswordResetProperties properties =
            new PasswordResetProperties(null, "noreply@swiftdrop.example", null);

    @Test
    void sendsResetLinkToRecipient() {
        new PasswordResetMailer(mailSender, properties).send(
                new PasswordResetRequested("inbox@example.test", "Nimal", "http://localhost:3000/reset-password?token=t"));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("inbox@example.test");
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@swiftdrop.example");
        assertThat(message.getValue().getText())
                .contains("Hi Nimal")
                .contains("http://localhost:3000/reset-password?token=t")
                .contains("30 minutes");
    }

    @Test
    void mailServerOutageDoesNotFailTheRequest() {
        doThrow(new MailSendException("connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> new PasswordResetMailer(mailSender, properties).send(
                new PasswordResetRequested("inbox@example.test", "Nimal", "link"))).doesNotThrowAnyException();
    }

    @Test
    void eventDoesNotPrintAddressOrLink() {
        assertThat(new PasswordResetRequested("inbox@example.test", "Nimal", "secret-link").toString())
                .doesNotContain("inbox@example.test").doesNotContain("secret-link");
    }

    @Test
    void propertiesFallBackToLocalDefaults() {
        PasswordResetProperties defaults = new PasswordResetProperties(" ", "", "  ");
        assertThat(defaults.resetUrl()).isEqualTo("http://localhost:3000/reset-password");
        assertThat(defaults.fromEmail()).isEqualTo("noreply@swiftdrop.lk");
        assertThat(defaults.defaultRecipient()).isNull();
    }
}
