package lk.swiftdrop.pricing.zone.dto;

import lk.swiftdrop.pricing.zone.entity.ZoneGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A zone as admins see it, including inactive ones. */
public record AdminZoneResponse(
        UUID id,
        String code,
        String name,
        ZoneGroup group,
        boolean active,
        List<String> districts,
        Instant createdAt,
        Instant updatedAt
) {
}
