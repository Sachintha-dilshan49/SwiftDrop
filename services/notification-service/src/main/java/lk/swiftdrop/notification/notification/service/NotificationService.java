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
import lk.swiftdrop.notification.notification.entity.NotificationType;
import lk.swiftdrop.notification.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/**
 * Handles customer-facing notifications triggered by ParcelStatusChanged events.
 *
 * Routing rules (architecture §6.3):
 *
 *   Customer booking (event.merchantId() == null):
 *     - Email  → customer  (contact from auth-service)
 *     - SMS    → recipient (contact from parcel-service) — only for SMS_STATUSES
 *
 *   Merchant order (event.merchantId() != null):
 *     - Email  → recipient (contact from parcel-service)
 *     - SMS    → recipient (contact from parcel-service) — only for SMS_STATUSES
 *
 * NOTE: parcel-service GET /internal/parcels/{id}/contacts is not yet on dev (Role 3 PR pending).
 * Until that endpoint lands, ParcelServiceClient returns Optional.empty() for all calls, which
 * means:
 *   - Customer bookings: email still goes to customer (auth-service). Recipient SMS is skipped.
 *   - Merchant orders:   email and SMS are skipped (no recipient contact available).
 * This is intentional — no fallback to wrong data. Behaviour is restored automatically when
 * the parcel endpoint is merged and the integration test passes.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final Set<ParcelStatus> SMS_STATUSES = Set.of(
            ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERED, ParcelStatus.FAILED);

    private final AuthServiceClient authServiceClient;
    private final ParcelServiceClient parcelServiceClient;
    private final EmailChannel emailChannel;
    private final SmsChannel smsChannel;
    private final NotificationRepository notificationRepository;
    private final String trackingBaseUrl;

    public NotificationService(AuthServiceClient authServiceClient,
                               ParcelServiceClient parcelServiceClient,
                               EmailChannel emailChannel,
                               SmsChannel smsChannel,
                               NotificationRepository notificationRepository,
                               @Value("${swiftdrop.notification.tracking-base-url}") String trackingBaseUrl) {
        this.authServiceClient = authServiceClient;
        this.parcelServiceClient = parcelServiceClient;
        this.emailChannel = emailChannel;
        this.smsChannel = smsChannel;
        this.notificationRepository = notificationRepository;
        this.trackingBaseUrl = trackingBaseUrl;
    }

    @Transactional
    public void processStatusChange(ParcelStatusChanged event) {
        boolean isMerchantOrder = event.merchantId() != null;

        if (isMerchantOrder) {
            processMerchantOrderNotification(event);
        } else {
            processCustomerBookingNotification(event);
        }
    }

    /**
     * Customer booking: email the booking customer, SMS the physical recipient.
     * Customer contact (email, name) comes from auth-service.
     * Recipient contact (phone) comes from parcel-service.
     */
    private void processCustomerBookingNotification(ParcelStatusChanged event) {
        var customerContact = authServiceClient.getUserContact(event.customerId());
        if (customerContact.isEmpty()) {
            log.warn("No customer contact for customerId={}, skipping email for eventId={}",
                    event.customerId(), event.eventId());
        } else {
            UserContactResponse customer = customerContact.get();
            sendEmail(event, customer.email(), customer.firstName());
        }

        if (SMS_STATUSES.contains(event.toStatus())) {
            var recipientContact = parcelServiceClient.getRecipientContacts(event.parcelId());
            if (recipientContact.isEmpty()) {
                log.info("No recipient contact from parcel-service for parcelId={} (endpoint may not be live yet)," +
                        " skipping recipient SMS for eventId={}", event.parcelId(), event.eventId());
            } else if (recipientContact.get().recipientPhone() != null) {
                sendSms(event, recipientContact.get().recipientPhone());
            }
        }
    }

    /**
     * Merchant order: email and SMS the physical recipient.
     * Both contacts come from parcel-service (recipientEmail + recipientPhone).
     */
    private void processMerchantOrderNotification(ParcelStatusChanged event) {
        var recipientContact = parcelServiceClient.getRecipientContacts(event.parcelId());
        if (recipientContact.isEmpty()) {
            log.info("No recipient contact from parcel-service for parcelId={} (endpoint may not be live yet)," +
                    " skipping merchant order notifications for eventId={}", event.parcelId(), event.eventId());
            return;
        }

        RecipientContactResponse recipient = recipientContact.get();

        if (recipient.recipientEmail() != null) {
            sendEmail(event, recipient.recipientEmail(), recipient.recipientName());
        }

        if (SMS_STATUSES.contains(event.toStatus()) && recipient.recipientPhone() != null) {
            sendSms(event, recipient.recipientPhone());
        }
    }

    private void sendEmail(ParcelStatusChanged event, String toEmail, String firstName) {
        String subject = buildEmailSubject(event);
        String body = buildEmailBody(event, firstName);
        Notification notification = new Notification(
                event.eventId(), event.parcelId(), event.trackingNo(),
                event.customerId(), NotificationType.EMAIL, toEmail,
                subject, body, NotificationStatus.SENT);
        try {
            emailChannel.send(toEmail, subject, body);
        } catch (NotificationSendException e) {
            notification.markFailed(e.getMessage());
            log.error("Email failed for eventId={}: {}", event.eventId(), e.getMessage());
        }
        notificationRepository.save(notification);
    }

    private void sendSms(ParcelStatusChanged event, String toPhone) {
        String body = buildSmsBody(event);
        Notification notification = new Notification(
                event.eventId(), event.parcelId(), event.trackingNo(),
                event.customerId(), NotificationType.SMS, toPhone,
                null, body, NotificationStatus.SENT);
        try {
            smsChannel.send(toPhone, null, body);
        } catch (NotificationSendException e) {
            notification.markFailed(e.getMessage());
            log.error("SMS failed for eventId={}: {}", event.eventId(), e.getMessage());
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
