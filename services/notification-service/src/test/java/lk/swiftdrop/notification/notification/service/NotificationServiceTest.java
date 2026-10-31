package lk.swiftdrop.notification.notification.service;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.notification.client.AuthServiceClient;
import lk.swiftdrop.notification.client.ParcelServiceClient;
import lk.swiftdrop.notification.client.dto.RecipientContactResponse;
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
    @Mock private ParcelServiceClient parcelServiceClient;
    @Mock private EmailChannel emailChannel;
    @Mock private SmsChannel smsChannel;
    @Mock private NotificationRepository notificationRepository;

    private NotificationService service;

    private static final UUID CUSTOMER_ID  = UUID.randomUUID();
    private static final UUID MERCHANT_ID  = UUID.randomUUID();
    private static final UUID PARCEL_ID    = UUID.randomUUID();

    private static final UserContactResponse CUSTOMER_CONTACT =
            new UserContactResponse("customer@example.com", "+94771234567", "Kamal");
    private static final RecipientContactResponse RECIPIENT_CONTACT =
            new RecipientContactResponse("Nimal", "+94779876543", "recipient@store.com");
    private static final RecipientContactResponse RECIPIENT_NO_EMAIL =
            new RecipientContactResponse("Nimal", "+94779876543", null);

    @BeforeEach
    void setUp() {
        service = new NotificationService(authServiceClient, parcelServiceClient,
                emailChannel, smsChannel, notificationRepository,
                "http://localhost:3000/track");
    }

    // ─── Customer booking (merchantId == null) ───────────────────────────────

    @Test
    void customerBooking_sendsEmailToCustomerOnEveryStatus() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID)).thenReturn(Optional.empty());

        service.processStatusChange(buildCustomerEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(emailChannel).send(eq("customer@example.com"), anyString(), anyString());
    }

    @Test
    void customerBooking_sendsSmsToRecipientOnOutForDelivery() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_NO_EMAIL));

        service.processStatusChange(buildCustomerEvent(ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY));

        verify(smsChannel).send(eq("+94779876543"), any(), anyString());
    }

    @Test
    void customerBooking_sendsSmsToRecipientOnDelivered() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_NO_EMAIL));

        service.processStatusChange(buildCustomerEvent(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERED));

        verify(smsChannel).send(eq("+94779876543"), any(), anyString());
    }

    @Test
    void customerBooking_sendsSmsToRecipientOnFailed() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_NO_EMAIL));

        service.processStatusChange(buildCustomerEvent(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.FAILED));

        verify(smsChannel).send(eq("+94779876543"), any(), anyString());
    }

    @Test
    void customerBooking_doesNotSendSmsForPickedUp() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));

        service.processStatusChange(buildCustomerEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(smsChannel, never()).send(anyString(), any(), anyString());
        verify(parcelServiceClient, never()).getRecipientContacts(any());
    }

    @Test
    void customerBooking_skipsEmailWhenCustomerContactNotFound() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID)).thenReturn(Optional.empty());

        service.processStatusChange(buildCustomerEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(emailChannel, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void customerBooking_skipsSmsWhenParcelServiceUnavailable() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID)).thenReturn(Optional.empty());

        service.processStatusChange(buildCustomerEvent(ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY));

        verify(emailChannel).send(anyString(), anyString(), anyString()); // email still goes
        verify(smsChannel, never()).send(anyString(), any(), anyString()); // sms skipped
    }

    @Test
    void customerBooking_recordsEmailAsFailedWhenSmtpFails() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID)).thenReturn(Optional.empty());
        doThrow(new NotificationSendException("SMTP error", new RuntimeException()))
                .when(emailChannel).send(anyString(), anyString(), anyString());

        service.processStatusChange(buildCustomerEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).contains("SMTP error");
    }

    @Test
    void customerBooking_emailBodyContainsTrackingUrl() {
        when(authServiceClient.getUserContact(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_CONTACT));
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID)).thenReturn(Optional.empty());

        service.processStatusChange(buildCustomerEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailChannel).send(anyString(), anyString(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("http://localhost:3000/track/SD-TEST-1234");
    }

    // ─── Merchant order (merchantId != null) ────────────────────────────────

    @Test
    void merchantOrder_sendsEmailToRecipientOnEveryStatus() {
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_CONTACT));

        service.processStatusChange(buildMerchantEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(emailChannel).send(eq("recipient@store.com"), anyString(), anyString());
        verify(authServiceClient, never()).getUserContact(any());
    }

    @Test
    void merchantOrder_sendsSmsToRecipientOnOutForDelivery() {
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_CONTACT));

        service.processStatusChange(buildMerchantEvent(ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY));

        verify(smsChannel).send(eq("+94779876543"), any(), anyString());
    }

    @Test
    void merchantOrder_doesNotSendSmsForPickedUp() {
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_CONTACT));

        service.processStatusChange(buildMerchantEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(emailChannel).send(anyString(), anyString(), anyString()); // email yes
        verify(smsChannel, never()).send(anyString(), any(), anyString()); // sms no
    }

    @Test
    void merchantOrder_skipsAllNotificationsWhenParcelServiceUnavailable() {
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID)).thenReturn(Optional.empty());

        service.processStatusChange(buildMerchantEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(emailChannel, never()).send(anyString(), anyString(), anyString());
        verify(smsChannel, never()).send(anyString(), any(), anyString());
        verify(authServiceClient, never()).getUserContact(any());
    }

    @Test
    void merchantOrder_skipsEmailWhenRecipientEmailIsNull() {
        when(parcelServiceClient.getRecipientContacts(PARCEL_ID))
                .thenReturn(Optional.of(RECIPIENT_NO_EMAIL));

        service.processStatusChange(buildMerchantEvent(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));

        verify(emailChannel, never()).send(anyString(), anyString(), anyString());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ParcelStatusChanged buildCustomerEvent(ParcelStatus from, ParcelStatus to) {
        return new ParcelStatusChanged(
                UUID.randomUUID(), ParcelStatusChanged.TYPE, ParcelStatusChanged.VERSION,
                Instant.now(), PARCEL_ID, "SD-TEST-1234", CUSTOMER_ID,
                null, null, from, to, null, null);
    }

    private ParcelStatusChanged buildMerchantEvent(ParcelStatus from, ParcelStatus to) {
        return new ParcelStatusChanged(
                UUID.randomUUID(), ParcelStatusChanged.TYPE, ParcelStatusChanged.VERSION,
                Instant.now(), PARCEL_ID, "SD-TEST-1234", CUSTOMER_ID,
                MERCHANT_ID, null, from, to, null, null);
    }
}
