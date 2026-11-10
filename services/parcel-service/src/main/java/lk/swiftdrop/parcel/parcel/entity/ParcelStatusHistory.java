package lk.swiftdrop.parcel.parcel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lk.swiftdrop.common.event.ParcelStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parcel_status_history")
public class ParcelStatusHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParcelStatus status;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_by_role", length = 50)
    private String changedByRole;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "proof_reference", length = 255)
    private String proofReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ParcelStatusHistory() {
    }

    public ParcelStatusHistory(UUID id, Parcel parcel, ParcelStatus status,
                               UUID changedBy, String changedByRole, String note,
                               String proofReference) {
        this.id = id != null ? id : UUID.randomUUID();
        this.parcel = parcel;
        this.status = status;
        this.changedBy = changedBy;
        this.changedByRole = changedByRole;
        this.note = note;
        this.proofReference = proofReference;
    }

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Parcel getParcel() {
        return parcel;
    }

    public void setParcel(Parcel parcel) {
        this.parcel = parcel;
    }

    public ParcelStatus getStatus() {
        return status;
    }

    public void setStatus(ParcelStatus status) {
        this.status = status;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(String changedByRole) {
        this.changedByRole = changedByRole;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getProofReference() {
        return proofReference;
    }

    public void setProofReference(String proofReference) {
        this.proofReference = proofReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
