package lk.swiftdrop.notification.webhook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "parcel_id", nullable = false)
    private UUID parcelId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "webhook_url", nullable = false, length = 2000)
    private String webhookUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private WebhookDeliveryStatus status;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected WebhookDelivery() {}

    public WebhookDelivery(UUID eventId, UUID parcelId, UUID merchantId,
                           String webhookUrl, int attemptNumber, String requestBody) {
        this.eventId = eventId;
        this.parcelId = parcelId;
        this.merchantId = merchantId;
        this.webhookUrl = webhookUrl;
        this.attemptNumber = attemptNumber;
        this.requestBody = requestBody;
        this.status = WebhookDeliveryStatus.FAILED;
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public void markSuccess(int statusCode, String responseBody) {
        this.status = WebhookDeliveryStatus.SUCCESS;
        this.httpStatus = statusCode;
        this.responseBody = responseBody;
        this.updatedAt = Instant.now();
    }

    public void markFailed(int statusCode, String responseBody, String reason) {
        this.status = WebhookDeliveryStatus.FAILED;
        this.httpStatus = statusCode;
        this.responseBody = responseBody;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markParked(String reason) {
        this.status = WebhookDeliveryStatus.PARKED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getParcelId() { return parcelId; }
    public UUID getMerchantId() { return merchantId; }
    public String getWebhookUrl() { return webhookUrl; }
    public Integer getHttpStatus() { return httpStatus; }
    public int getAttemptNumber() { return attemptNumber; }
    public WebhookDeliveryStatus getStatus() { return status; }
    public String getRequestBody() { return requestBody; }
    public String getResponseBody() { return responseBody; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
