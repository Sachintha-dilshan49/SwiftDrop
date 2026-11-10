package lk.swiftdrop.pricing.zone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.swiftdrop.pricing.quote.dto.ValidationPatterns;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;

import java.util.List;

/**
 * A new zone. The code cannot be changed later: parcel-service stores it with every parcel.
 *
 * @param active optional, true when absent
 */
public record CreateZoneRequest(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = ValidationPatterns.ZONE, message = "Code " + ValidationPatterns.ZONE_MESSAGE)
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotNull(message = "Group is required")
        ZoneGroup group,

        Boolean active,

        @NotEmpty(message = "At least one district is required")
        @Size(max = 50, message = "At most 50 districts")
        List<@NotBlank(message = "District must not be blank")
             @Size(max = 100, message = "District must be at most 100 characters") String> districts
) {
}
