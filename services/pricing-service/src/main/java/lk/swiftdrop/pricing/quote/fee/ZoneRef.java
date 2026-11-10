package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.pricing.zone.entity.ZoneGroup;

/** The parts of a zone the fee depends on: its code and its group. */
public record ZoneRef(String code, ZoneGroup group) {
}
