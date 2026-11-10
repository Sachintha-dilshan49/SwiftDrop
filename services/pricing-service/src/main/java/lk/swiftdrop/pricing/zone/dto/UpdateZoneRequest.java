package lk.swiftdrop.pricing.zone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;

import java.util.List;

/** Replaces everything about a zone except its code. */
public record UpdateZoneRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotNull(message = "Group is required")
        ZoneGroup group,

        @NotNull(message = "Active is required")
        Boolean active,

        @NotEmpty(message = "At least one district is required")
        @Size(max = 50, message = "At most 50 districts")
        List<@NotBlank(message = "District must not be blank")
             @Size(max = 100, message = "District must be at most 100 characters") String> districts
) {
}
