package lk.swiftdrop.pricing.zone.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A delivery zone. Its code never changes, because parcel-service stores it with every parcel. */
@Entity
@Table(name = "zones")
public class Zone {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_group", nullable = false, length = 20)
    private ZoneGroup group;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "zone_districts", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "district_name", nullable = false, length = 100)
    @OrderBy
    private List<String> districts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Zone() {
    }

    public Zone(String code, String name, ZoneGroup group, boolean active, List<String> districts) {
        this.id = UUID.randomUUID();
        this.code = code;
        update(name, group, active, districts);
    }

    /** Everything but the code can change. */
    public void update(String name, ZoneGroup group, boolean active, List<String> districts) {
        this.name = name;
        this.group = group;
        this.active = active;
        this.districts.clear();
        this.districts.addAll(districts);
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ZoneGroup getGroup() {
        return group;
    }

    public boolean isActive() {
        return active;
    }

    public List<String> getDistricts() {
        return List.copyOf(districts);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
