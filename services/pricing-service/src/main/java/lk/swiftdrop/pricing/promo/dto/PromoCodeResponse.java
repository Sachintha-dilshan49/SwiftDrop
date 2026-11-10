package lk.swiftdrop.pricing.promo.dto;

import lk.swiftdrop.pricing.promo.entity.PromoCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** @param validTo null when the code does not expire */
public record PromoCodeResponse(
        UUID id,
        String code,
        BigDecimal discountAmount,
        String currency,
        Instant validFrom,
        Instant validTo,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static PromoCodeResponse from(PromoCode promo) {
        return new PromoCodeResponse(promo.getId(), promo.getCode(), promo.getDiscountAmount(), promo.getCurrency(),
                promo.getValidFrom(), promo.getValidTo(), promo.isActive(), promo.getCreatedAt(),
                promo.getUpdatedAt());
    }
}
