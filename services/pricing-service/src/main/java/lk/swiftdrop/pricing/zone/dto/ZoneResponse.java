package lk.swiftdrop.pricing.zone.dto;

import lk.swiftdrop.pricing.zone.entity.ZoneGroup;

import java.util.List;

/** A zone as customers see it. */
public record ZoneResponse(
        String code,
        String name,
        ZoneGroup group,
        List<String> districts
) {
}
