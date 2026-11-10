package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.quote.model.RouteType;
import lk.swiftdrop.pricing.rule.entity.PricingRule;
import lk.swiftdrop.pricing.rule.entity.RuleType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The active pricing rules, loaded once per quote so every fee component sees the same amounts. A rule that is
 * missing here is inactive: a size or speed without a rule is not offered.
 */
public final class PricingRules {

    /** A size's surcharge and the heaviest parcel it takes. */
    public record SizeRule(BigDecimal surcharge, BigDecimal maxWeightKg) {
    }

    private final Map<RouteType, BigDecimal> routeBaseFees = new EnumMap<>(RouteType.class);
    private final Map<ParcelSize, SizeRule> sizes = new EnumMap<>(ParcelSize.class);
    private final Map<DeliverySpeed, BigDecimal> speeds = new EnumMap<>(DeliverySpeed.class);
    private BigDecimal extraKgFee;

    private PricingRules() {
    }

    /** Builds the rule book from the active rules; inactive rules passed in are ignored. */
    public static PricingRules of(List<PricingRule> rules) {
        PricingRules book = new PricingRules();
        for (PricingRule rule : rules) {
            if (!rule.isActive()) {
                continue;
            }
            String key = rule.getRuleKey();
            switch (rule.getRuleType()) {
                case ROUTE_BASE -> book.routeBaseFees.put(RouteType.valueOf(key), rule.getAmount());
                case EXTRA_KG -> book.extraKgFee = rule.getAmount();
                case SIZE -> book.sizes.put(ParcelSize.valueOf(key),
                        new SizeRule(rule.getAmount(), rule.getMaxWeightKg()));
                case SPEED -> book.speeds.put(DeliverySpeed.valueOf(key), rule.getAmount());
            }
        }
        return book;
    }

    /** Every quote needs a base fee; a missing one is a configuration error, not the customer's fault. */
    public BigDecimal routeBaseFee(RouteType routeType) {
        BigDecimal fee = routeBaseFees.get(routeType);
        if (fee == null) {
            throw new IllegalStateException("No active " + RuleType.ROUTE_BASE + " rule for " + routeType);
        }
        return fee;
    }

    /** Every quote needs the per-kg fee; a missing one is a configuration error, not the customer's fault. */
    public BigDecimal extraKgFee() {
        if (extraKgFee == null) {
            throw new IllegalStateException("No active " + RuleType.EXTRA_KG + " rule");
        }
        return extraKgFee;
    }

    public Optional<SizeRule> size(ParcelSize size) {
        return Optional.ofNullable(sizes.get(size));
    }

    public Optional<BigDecimal> speed(DeliverySpeed speed) {
        return Optional.ofNullable(speeds.get(speed));
    }
}
