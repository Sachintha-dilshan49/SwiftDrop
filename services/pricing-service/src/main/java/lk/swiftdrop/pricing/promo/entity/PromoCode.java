package lk.swiftdrop.pricing.promo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A fixed amount off the fee. The code is stored in upper case and never changes. */
@Entity
@Table(name = "promo_codes")
public class PromoCode {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, length = 30)
    private String code;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** {@code char(3)} as conventions §3 require; Hibernate would expect varchar for a String otherwise. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "LKR";

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /** Null: the code does not expire. */
    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected PromoCode() {
    }

    public PromoCode(String code, BigDecimal discountAmount, Instant validFrom, Instant validTo, boolean active) {
        this.id = UUID.randomUUID();
        this.code = code;
        update(discountAmount, validFrom, validTo, active);
    }

    public void update(BigDecimal discountAmount, Instant validFrom, Instant validTo, boolean active) {
        this.discountAmount = discountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = active;
    }

    /** {@code validFrom} itself is included. */
    public boolean hasStartedAt(Instant now) {
        return !now.isBefore(validFrom);
    }

    /** {@code validTo} itself is excluded: at that instant the code has ended. */
    public boolean hasEndedAt(Instant now) {
        return validTo != null && !now.isBefore(validTo);
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

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
