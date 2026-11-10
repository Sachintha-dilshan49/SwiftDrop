package lk.swiftdrop.pricing.zone.mapper;

import lk.swiftdrop.pricing.zone.dto.ZoneResponse;
import lk.swiftdrop.pricing.zone.entity.Zone;

/** Hand-written mappers, one place for every zone DTO. */
public final class ZoneMapper {

    private ZoneMapper() {
    }

    public static ZoneResponse toResponse(Zone zone) {
        return new ZoneResponse(zone.getCode(), zone.getName(), zone.getGroup(), zone.getDistricts());
    }
}
