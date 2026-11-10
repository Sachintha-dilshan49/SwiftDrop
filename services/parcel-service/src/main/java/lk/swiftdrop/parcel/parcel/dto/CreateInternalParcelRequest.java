package lk.swiftdrop.parcel.parcel.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A merchant order from merchant-service ({@code POST /internal/parcels}, docs/diagrams/seq-internal-parcel-api).
 * There is no fee field: a fee sent by a merchant is ignored, the fee always comes from pricing-service.
 */
public record CreateInternalParcelRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotBlank(message = "Pickup address is required")
        @Size(max = 500, message = "Pickup address cannot exceed 500 characters")
        String pickupAddress,

        @NotBlank(message = "Pickup zone is required")
        @Pattern(regexp = ValidationPatterns.ZONE, message = "Pickup zone " + ValidationPatterns.ZONE_MESSAGE)
        String pickupZone,

        @NotBlank(message = "Drop-off address is required")
        @Size(max = 500, message = "Drop-off address cannot exceed 500 characters")
        String dropoffAddress,

        @NotBlank(message = "Drop-off zone is required")
        @Pattern(regexp = ValidationPatterns.ZONE, message = "Drop-off zone " + ValidationPatterns.ZONE_MESSAGE)
        String dropoffZone,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 150, message = "Recipient name cannot exceed 150 characters")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String recipientPhone,

        // Merchant orders email the recipient, so the address is required here
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email must be a valid email address")
        @Size(max = 254, message = "Recipient email cannot exceed 254 characters")
        String recipientEmail,

        @NotNull(message = "Size category is required")
        ParcelSize size,

        @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be greater than 0")
        @Digits(integer = 4, fraction = 2, message = "Weight must be at most 9999.99 kg with up to 2 decimals")
        BigDecimal weightKg,

        @NotNull(message = "Delivery speed is required")
        DeliverySpeed speed
) {
}
