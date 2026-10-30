package lk.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MerchantCreateOrderRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(max = 150, message = "Recipient name must not exceed 150 characters")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Size(max = 30, message = "Recipient phone must not exceed 30 characters")
        String recipientPhone,

        @NotBlank(message = "Pickup address is required")
        @Size(max = 500, message = "Pickup address must not exceed 500 characters")
        String pickupAddress,

        @NotBlank(message = "Dropoff address is required")
        @Size(max = 500, message = "Dropoff address must not exceed 500 characters")
        String dropoffAddress,

        @NotNull(message = "Weight in kg is required")
        @Positive(message = "Weight must be greater than zero")
        BigDecimal weightKg,

        @NotBlank(message = "Size is required (SMALL, MEDIUM, LARGE)")
        String size,

        @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
        String idempotencyKey
) {}
