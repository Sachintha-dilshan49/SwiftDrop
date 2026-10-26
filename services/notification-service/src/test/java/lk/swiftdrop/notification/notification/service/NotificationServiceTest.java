package lk.swiftdrop.notification.notification.service;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.notification.client.AuthServiceClient;
import lk.swiftdrop.notification.client.dto.UserContactResponse;
import lk.swiftdrop.notification.notification.channel.EmailChannel;
import lk.swiftdrop.notification.notification.channel.NotificationSendException;
import lk.swiftdrop.notification.notification.channel.SmsChannel;
import lk.swiftdrop.notification.notification.entity.Notification;
import lk.swiftdrop.notification.notification.entity.NotificationStatus;
import lk.swiftdrop.notification.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private AuthServiceClient authServiceClient;
    @Mock private EmailChannel emailChannel;
    @Mock private SmsChannel smsChannel;
    @Mock private NotificationRepository notificationRepository;

    private NotificationService service;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PARCEL_ID = UUID.randomUUID();
    private static final UserContactResponse CONTACT =
            new UserContactResponse("user@example.com", "+94771234567", "Kamal");

    @BeforeEach
    void setUp() {
        service = new NotificationService(authServiceClient, emailChannel, smsChannel,
                notificationRepository, "http://localhost:3000/track");
    }

    @Test
    void sendsEmailOnEveryStatusChange() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        service.processStatusChange(buildEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
        verify(emailChannel).send(eq("user@example.com"), anyString(), anyString());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendsSmsOnOutForDelivery() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        service.processStatusChange(buildEvent(ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY));
        verify(smsChannel).send(eq("+94771234567"), any(), anyString());
    }

    @Test
    void sendsSmsOnDelivered() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        service.processStatusChange(buildEvent(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERED));
        verify(smsChannel).send(eq("+94771234567"), any(), anyString());
    }

    @Test
    void sendsSmsOnFailed() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        service.processStatusChange(buildEvent(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.FAILED));
        verify(smsChannel).send(eq("+94771234567"), any(), anyString());
    }

    @Test
    void doesNotSendSmsForPickedUp() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        service.processStatusChange(buildEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
        verify(smsChannel, never()).send(anyString(), any(), anyString());
    }

    @Test
    void skipsNotificationWhenContactNotFound() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.empty());
        service.processStatusChange(buildEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
        verify(emailChannel, never()).send(anyString(), anyString(), anyString());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void recordsNotificationAsFailedWhenEmailFails() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        doThrow(new NotificationSendException("SMTP error", new RuntimeException()))
                .when(emailChannel).send(anyString(), anyString(), anyString());
        service.processStatusChange(buildEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).contains("SMTP error");
    }

    @Test
    void emailBodyContainsTrackingUrl() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CONTACT));
        service.processStatusChange(buildEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailChannel).send(anyString(), anyString(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("http://localhost:3000/track/SD-TEST-1234");
    }

    private ParcelStatusChanged buildEvent(ParcelStatus from, ParcelStatus to) {
        return new ParcelStatusChanged(
                UUID.randomUUID(), ParcelStatusChanged.TYPE, ParcelStatusChanged.VERSION,
                Instant.now(), PARCEL_ID, "SD-TEST-1234", CUSTOMER_ID,
                null, null, from, to, null, null);
    }
}
