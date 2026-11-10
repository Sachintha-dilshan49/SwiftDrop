package lk.swiftdrop.pricing.quote.service;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.promo.repository.PromoCodeRepository;
import lk.swiftdrop.pricing.quote.dto.PricingQuoteResponse;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.fee.FeeBreakdown;
import lk.swiftdrop.pricing.quote.fee.FeeCalculator;
import lk.swiftdrop.pricing.quote.fee.FeeContext;
import lk.swiftdrop.pricing.quote.fee.PricingRules;
import lk.swiftdrop.pricing.quote.fee.ZoneRef;
import lk.swiftdrop.pricing.rule.repository.PricingRuleRepository;
import lk.swiftdrop.pricing.zone.entity.Zone;
import lk.swiftdrop.pricing.zone.repository.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

/**
 * Prices a delivery: looks up the zones, the promo code and the active rules, then lets {@link FeeCalculator}
 * do the arithmetic. The binding quote and the public estimate both come from here, so they always agree.
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private final ZoneRepository zoneRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final FeeCalculator feeCalculator;
    private final Clock clock;

    public QuoteService(ZoneRepository zoneRepository, PricingRuleRepository pricingRuleRepository,
                        PromoCodeRepository promoCodeRepository, FeeCalculator feeCalculator, Clock clock) {
        this.zoneRepository = zoneRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.feeCalculator = feeCalculator;
        this.clock = clock;
    }

    /** @throws ValidationException when the request cannot be priced; the detail is safe to show the customer */
    @Transactional(readOnly = true)
    public PricingQuoteResponse quote(QuoteRequest request) {
        ZoneRef pickup = servedZone(request.pickupZone(), "pickupZone", "Pickup");
        ZoneRef dropoff = servedZone(request.dropoffZone(), "dropoffZone", "Drop-off");
        String promoCode = normalisedPromoCode(request.promoCode());
        PromoCode promo = promoCode == null ? null : promoCodeRepository.findByCode(promoCode).orElse(null);

        FeeBreakdown fee = feeCalculator.calculate(new FeeContext(pickup, dropoff, request.weightKg(), request.size(),
                request.speed(), promoCode, promo, PricingRules.of(pricingRuleRepository.findByActiveTrue()),
                clock.instant()));

        log.debug("Quoted {} {} {} kg {} {}: total {}", fee.routeType(), request.size(), request.weightKg(),
                request.speed(), promoCode == null ? "without promo" : "with promo", fee.total());
        return PricingQuoteResponse.from(fee);
    }

    private ZoneRef servedZone(String code, String field, String label) {
        return zoneRepository.findByCode(code)
                .filter(Zone::isActive)
                .map(zone -> new ZoneRef(zone.getCode(), zone.getGroup()))
                .orElseThrow(() -> new ValidationException(label + " zone " + code + " is not served.",
                        List.of(new FieldErrorDetail(field, "is not a served zone"))));
    }

    /** Codes are stored in upper case; blank means none. */
    private static String normalisedPromoCode(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return null;
        }
        return promoCode.trim().toUpperCase(Locale.ROOT);
    }
}
