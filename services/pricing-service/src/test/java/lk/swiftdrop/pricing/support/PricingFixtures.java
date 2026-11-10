package lk.swiftdrop.pricing.support;

import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.quote.fee.FeeContext;
import lk.swiftdrop.pricing.quote.fee.PricingRules;
import lk.swiftdrop.pricing.quote.fee.ZoneRef;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.rule.entity.PricingRule;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Test data matching the seed migrations (V4, V5) and the worked example of docs/architecture.md §8. */
public final class PricingFixtures {

    public static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");

    public static final ZoneRef COLOMBO_CENTRAL = new ZoneRef("COLOMBO_CENTRAL", ZoneGroup.COLOMBO);
    public static final ZoneRef COLOMBO_SOUTH = new ZoneRef("COLOMBO_SOUTH", ZoneGroup.COLOMBO);
    public static final ZoneRef KOTTE_RAJAGIRIYA = new ZoneRef("KOTTE_RAJAGIRIYA", ZoneGroup.SUBURBS);
    public static final ZoneRef NUGEGODA_MAHARAGAMA = new ZoneRef("NUGEGODA_MAHARAGAMA", ZoneGroup.SUBURBS);
    public static final ZoneRef KANDY = new ZoneRef("KANDY", ZoneGroup.INTER_CITY);
    public static final ZoneRef GALLE = new ZoneRef("GALLE", ZoneGroup.INTER_CITY);

    private PricingFixtures() {
    }

    /** The rules of V5, all active. */
    public static List<PricingRule> seedRules() {
        return new ArrayList<>(List.of(
                rule(RuleType.ROUTE_BASE, "INTER_CITY", "650.00", null),
                rule(RuleType.ROUTE_BASE, "SAME_ZONE", "300.00", null),
                rule(RuleType.ROUTE_BASE, "NEIGHBOURING", "350.00", null),
                rule(RuleType.ROUTE_BASE, "COLOMBO_SUBURBS", "400.00", null),
                rule(RuleType.EXTRA_KG, "PER_KG", "40.00", null),
                rule(RuleType.SIZE, "SMALL", "0.00", "5.00"),
                rule(RuleType.SIZE, "MEDIUM", "100.00", "10.00"),
                rule(RuleType.SIZE, "LARGE", "250.00", "20.00"),
                rule(RuleType.SIZE, "EXTRA_LARGE", "500.00", "40.00"),
                rule(RuleType.SPEED, "EXPRESS", "100.00", null),
                rule(RuleType.SPEED, "STANDARD", "0.00", null),
                rule(RuleType.SPEED, "NEXT_DAY", "0.00", null)));
    }

    public static PricingRules seedRuleBook() {
        return PricingRules.of(seedRules());
    }

    /** The seed rules without the ones matching {@code removed}, as if those were inactive. */
    public static PricingRules ruleBookWithout(Predicate<PricingRule> removed) {
        List<PricingRule> rules = seedRules();
        rules.removeIf(removed);
        return PricingRules.of(rules);
    }

    public static PricingRule rule(RuleType type, String key, String amount, String maxWeightKg) {
        return new PricingRule(type, key, new BigDecimal(amount),
                maxWeightKg == null ? null : new BigDecimal(maxWeightKg), true);
    }

    /** DROP100 as seeded: 100 LKR off, valid from 2026-01-01, no end date. */
    public static PromoCode drop100() {
        return new PromoCode("DROP100", new BigDecimal("100.00"), Instant.parse("2026-01-01T00:00:00Z"), null, true);
    }

    /** The worked example without a promo: COLOMBO_SOUTH to COLOMBO_CENTRAL, 1.2 kg, SMALL, EXPRESS. */
    public static FeeContext workedExample() {
        return context(COLOMBO_SOUTH, COLOMBO_CENTRAL, "1.2", ParcelSize.SMALL, DeliverySpeed.EXPRESS);
    }

    public static FeeContext context(ZoneRef pickup, ZoneRef dropoff, String weightKg, ParcelSize size,
                                     DeliverySpeed speed) {
        return new FeeContext(pickup, dropoff, new BigDecimal(weightKg), size, speed, null, null,
                seedRuleBook(), NOW);
    }

    public static FeeContext withPromo(FeeContext context, String code, PromoCode promo) {
        return new FeeContext(context.pickup(), context.dropoff(), context.weightKg(), context.size(),
                context.speed(), code, promo, context.rules(), context.now());
    }

    public static FeeContext withRules(FeeContext context, PricingRules rules) {
        return new FeeContext(context.pickup(), context.dropoff(), context.weightKg(), context.size(),
                context.speed(), context.promoCode(), context.promo(), rules, context.now());
    }

    public static FeeContext withNow(FeeContext context, Instant now) {
        return new FeeContext(context.pickup(), context.dropoff(), context.weightKg(), context.size(),
                context.speed(), context.promoCode(), context.promo(), context.rules(), now);
    }
}
