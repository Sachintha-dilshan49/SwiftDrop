package lk.swiftdrop.dispatch.assignment;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "parcel_id", nullable = false)
    private UUID parcelId;

    @Column(name = "courier_id", nullable = false)
    private UUID courierId;

    @Column(name = "assigned_by_user_id", nullable = false)
    private UUID assignedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Embedded
    private ParcelDetails parcelDetails;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Assignment() {
        this.id = UUID.randomUUID();
    }

    @PrePersist
    public void prePersist() {
        this.assignedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getParcelId() {
        return parcelId;
    }

    public void setParcelId(UUID parcelId) {
        this.parcelId = parcelId;
    }

    public UUID getCourierId() {
        return courierId;
    }

    public void setCourierId(UUID courierId) {
        this.courierId = courierId;
    }

    public UUID getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(UUID assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    /** Null for assignments made before parcel details were copied (V2). */
    public ParcelDetails getParcelDetails() {
        return parcelDetails;
    }

    public void setParcelDetails(ParcelDetails parcelDetails) {
        this.parcelDetails = parcelDetails;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
