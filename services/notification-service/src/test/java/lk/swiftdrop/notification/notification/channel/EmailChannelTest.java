package lk.swiftdrop.notification.notification.channel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailChannelTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailChannel emailChannel;

    @Test
    void sendsEmailWithCorrectFields() {
        ReflectionTestUtils.setField(emailChannel, "fromEmail", "noreply@swiftdrop.lk");
        emailChannel.send("customer@example.com", "Test Subject", "Test Body");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("noreply@swiftdrop.lk");
        assertThat(sent.getTo()).containsExactly("customer@example.com");
        assertThat(sent.getSubject()).isEqualTo("Test Subject");
        assertThat(sent.getText()).isEqualTo("Test Body");
    }

    @Test
    void throwsNotificationSendExceptionOnMailFailure() {
        ReflectionTestUtils.setField(emailChannel, "fromEmail", "noreply@swiftdrop.lk");
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> emailChannel.send("a@b.com", "Sub", "Body"))
                .isInstanceOf(NotificationSendException.class)
                .hasMessageContaining("Email send failed");
    }

    @Test
    void channelNameIsEmail() {
        assertThat(emailChannel.channelName()).isEqualTo("EMAIL");
    }
}
