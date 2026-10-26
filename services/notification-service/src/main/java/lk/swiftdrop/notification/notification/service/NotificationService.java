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
import lk.swiftdrop.notification.notification.entity.NotificationType;
import lk.swiftdrop.notification.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final Set<ParcelStatus> SMS_STATUSES = Set.of(
            ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERED, ParcelStatus.FAILED);

    private final AuthServiceClient authServiceClient;
    private final EmailChannel emailChannel;
    private final SmsChannel smsChannel;
    private final NotificationRepository notificationRepository;
    private final String trackingBaseUrl;

    public NotificationService(AuthServiceClient authServiceClient,
                               EmailChannel emailChannel,
                               SmsChannel smsChannel,
                               NotificationRepository notificationRepository,
                               @Value("${swiftdrop.notification.tracking-base-url}") String trackingBaseUrl) {
        this.authServiceClient = authServiceClient;
        this.emailChannel = emailChannel;
        this.smsChannel = smsChannel;
        this.notificationRepository = notificationRepository;
        this.trackingBaseUrl = trackingBaseUrl;
    }

    @Transactional
    public void processStatusChange(ParcelStatusChanged event) {
        var contact = authServiceClient.getUserContact(event.customerId());
        if (contact.isEmpty()) {
            log.warn("No contact found for customer {}, skipping notification for event {}",
                    event.customerId(), event.eventId());
            return;
        }
        UserContactResponse user = contact.get();

        sendEmail(event, user);

        if (SMS_STATUSES.contains(event.toStatus()) && user.phone() != null) {
            sendSms(event, user);
        }
    }

    private void sendEmail(ParcelStatusChanged event, UserContactResponse user) {
        String subject = buildEmailSubject(event);
        String body = buildEmailBody(event, user.firstName());
        Notification notification = new Notification(
                event.eventId(), event.parcelId(), event.trackingNo(),
                event.customerId(), NotificationType.EMAIL, user.email(),
                subject, body, NotificationStatus.SENT);
        try {
            emailChannel.send(user.email(), subject, body);
        } catch (NotificationSendException e) {
            notification.markFailed(e.getMessage());
            log.error("Email failed for event {}: {}", event.eventId(), e.getMessage());
        }
        notificationRepository.save(notification);
    }

    private void sendSms(ParcelStatusChanged event, UserContactResponse user) {
        String body = buildSmsBody(event);
        Notification notification = new Notification(
                event.eventId(), event.parcelId(), event.trackingNo(),
                event.customerId(), NotificationType.SMS, user.phone(),
                null, body, NotificationStatus.SENT);
        try {
            smsChannel.send(user.phone(), null, body);
        } catch (NotificationSendException e) {
            notification.markFailed(e.getMessage());
            log.error("SMS failed for event {}: {}", event.eventId(), e.getMessage());
        }
        notificationRepository.save(notification);
    }

    private String buildEmailSubject(ParcelStatusChanged event) {
        return "SwiftDrop – Your parcel %s is now %s".formatted(
                event.trackingNo(), formatStatus(event.toStatus()));
    }

    private String buildEmailBody(ParcelStatusChanged event, String firstName) {
        String noteSection = event.note() != null ? "Note: " + event.note() + "\n\n" : "";
        String failSection = event.failureReason() != null ? "Reason: " + event.failureReason() + "\n\n" : "";
        return """
                Hi %s,

                Your parcel %s has been updated.

                Status: %s → %s
                %s%sTrack your parcel: %s/%s

                – SwiftDrop
                """.formatted(
                firstName != null ? firstName : "Customer",
                event.trackingNo(),
                formatStatus(event.fromStatus()),
                formatStatus(event.toStatus()),
                noteSection, failSection,
                trackingBaseUrl, event.trackingNo());
    }

    private String buildSmsBody(ParcelStatusChanged event) {
        return "SwiftDrop: Parcel %s is %s. Track: %s/%s".formatted(
                event.trackingNo(), formatStatus(event.toStatus()),
                trackingBaseUrl, event.trackingNo());
    }

    private String formatStatus(ParcelStatus status) {
        if (status == null) return "Unknown";
        return status.name().replace('_', ' ').toLowerCase();
    }
}
