package lk.swiftdrop.pricing.rule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One amount the fee is made of. The set of rules comes from the migrations; admins change only the amount
 * and whether the rule is active.
 */
@Entity
@Table(name = "pricing_rules")
public class PricingRule {

    public static final String EXTRA_KG_KEY = "PER_KG";

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, updatable = false, length = 20)
    private RuleType ruleType;

    @Column(name = "rule_key", nullable = false, updatable = false, length = 30)
    private String ruleKey;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** {@code char(3)} as conventions §3 require; Hibernate would expect varchar for a String otherwise. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "LKR";

    /** Only for {@link RuleType#SIZE}: the heaviest parcel this size takes. */
    @Column(name = "max_weight_kg", precision = 6, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected PricingRule() {
    }

    public PricingRule(RuleType ruleType, String ruleKey, BigDecimal amount, BigDecimal maxWeightKg, boolean active) {
        this.id = UUID.randomUUID();
        this.ruleType = ruleType;
        this.ruleKey = ruleKey;
        this.amount = amount;
        this.maxWeightKg = maxWeightKg;
        this.active = active;
    }

    public void update(BigDecimal amount, boolean active) {
        this.amount = amount;
        this.active = active;
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

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
