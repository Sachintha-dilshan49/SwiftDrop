package lk.swiftdrop.dispatch.proof;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_proofs")
public class DeliveryProof {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private UUID assignmentId;

    @Column(name = "object_key", nullable = false, updatable = false)
    private String objectKey;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected DeliveryProof() {
    }

    public DeliveryProof(UUID assignmentId, String objectKey) {
        this.id = UUID.randomUUID();
        this.assignmentId = assignmentId;
        this.objectKey = objectKey;
    }

    @PrePersist
    public void prePersist() {
        this.uploadedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public String getObjectKey() {
        return objectKey;
    }
}
